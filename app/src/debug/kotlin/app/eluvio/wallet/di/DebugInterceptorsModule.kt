package app.eluvio.wallet.di

import com.moczul.ok2curl.Configuration
import com.moczul.ok2curl.CurlInterceptor
import com.moczul.ok2curl.Header
import com.moczul.ok2curl.logger.Logger
import com.moczul.ok2curl.modifier.HeaderModifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber


@Module
@InstallIn(SingletonComponent::class)
object DebugInterceptorsModule {
    @Provides
    @IntoSet
    fun provideLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }
            .setLevel(HttpLoggingInterceptor.Level.BASIC)
    }

    @Provides
    @IntoSet
    fun provideCurlInterceptor(): Interceptor {
        val headerFilter = object : HeaderModifier {
            private val allowlist = listOf("Authorization", "Accept", "Content-Type", "User-Agent")
            override fun matches(header: Header): Boolean {
                // Catch all headers that are not interesting
                return header.name !in allowlist
            }

            override fun modify(header: Header): Header? {
                // And map them to null
                return null
            }
        }

        return CurlInterceptor(
            configuration = Configuration(
                headerModifiers = listOf(headerFilter)
            ),
            logger = object : Logger {
                override fun log(message: String) {
                    Timber.tag("OkHttp").w(
                        reorderHeaders(message)
                    )
                }
            }
        )
    }

    private fun reorderHeaders(curlCommand: String): String {
        val headerRegex = Regex("""-H\s+(['"][^'"]+['"])""")
        val authHeaderRegex = Regex("""-H\s+(['"]Authorization:[^'"]+['"])""")

        val allHeaders = headerRegex.findAll(curlCommand).map { it.value.trim() }.toList()
        val authHeaders = allHeaders.filter { authHeaderRegex.matches(it) }
        val nonAuthHeaders = allHeaders.filterNot { authHeaderRegex.matches(it) }

        var modifiedCommand = curlCommand
        allHeaders.forEach { header ->
            modifiedCommand = modifiedCommand.replace(Regex("\\s*${Regex.escape(header)}\\s*"), " ")
        }
        modifiedCommand = modifiedCommand.replace(Regex("\\s+"), " ").trim()

        val reorderedHeaders = (nonAuthHeaders + authHeaders).joinToString(" ")

        return if (reorderedHeaders.isNotEmpty()) {
            "$modifiedCommand $reorderedHeaders".trim()
        } else {
            modifiedCommand
        }
    }

}
