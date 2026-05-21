package app.eluvio.wallet.util.rx

/**
 * A container object which may or may not contain a non-null value.
 * If a value is present, `isPresent()` will return `true` and
 * `get()` will return the value.
 *
 *
 *
 * Additional methods that depend on the presence or absence of a contained
 * value are provided, such as [orDefault()][.orDefault].
 *
 *
 *
 * This is a [value-based](../lang/doc-files/ValueBased.html)
 * class; use of identity-sensitive operations (including reference equality
 * (`==`), identity hash code, or synchronization) on instances of
 * `Optional` may have unpredictable results and should be avoided.
 */
class Optional<T> private constructor(
    /**
     * If non-null, the value; if null, indicates no value is present
     */
    private val value: T?
) {
    companion object {
        /**
         * Common instance for `empty()`.
         */
        private val EMPTY: Optional<*> = Optional<Any>(null)

        /**
         * Returns an empty `Optional` instance.  No value is present for this
         * Optional.
         *
         * @param <T> Type of the non-existent value
         * @return an empty `Optional`
         * @apiNote Though it may be tempting to do so, avoid testing if an object
         * is empty by comparing with `==` against instances returned by
         * `Option.empty()`. There is no guarantee that it is a singleton.
         * Instead, use [.isPresent].
        </T> */
        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): Optional<T> {
            return EMPTY as Optional<T>
        }

        /**
         * Returns an `Optional` describing the specified value, if non-null,
         * otherwise returns an empty `Optional`.
         *
         * @param <T>   the class of the value
         * @param value the possibly-null value to describe
         * @return an `Optional` with a present value if the specified value
         * is non-null, otherwise an empty `Optional`
        </T> */
        fun <T> of(value: T?): Optional<T> {
            return if (value == null) empty() else Optional(value)
        }
    }

    /**
     * If a value is present in this `Optional`, returns the value,
     * otherwise throws `NoSuchElementException`.
     *
     * @return the non-null value held by this `Optional`
     * @throws NoSuchElementException if there is no value present
     * @see Optional.isPresent
     */
    fun get(): T {
        if (value == null) {
            throw NoSuchElementException("No value present")
        }
        return value
    }

    val isPresent: Boolean
        /**
         * Return `true` if there is a value present, otherwise `false`.
         *
         * @return `true` if there is a value present, otherwise `false`
         */
        get() = value != null

    /**
     * Return the value if present, otherwise return `other`.
     *
     * @param other the value to be returned if there is no value present, may
     * be null
     * @return the value, if present, otherwise `other`
     */
    fun orDefault(other: T?): T? {
        return value ?: other
    }

    /**
     * Indicates whether some other object is "equal to" this Optional. The
     * other object is considered equal if:
     *
     *  * it is also an `Optional` and;
     *  * both instances have no value present or;
     *  * the present values are "equal to" each other via `equals()`.
     *
     *
     * @param obj an object to be tested for equality
     * @return {code true} if the other object is "equal to" this object
     * otherwise `false`
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }

        if (obj !is Optional<*>) {
            return false
        }

        return value != null && value == obj.value
    }

    /**
     * Returns the hash code value of the present value, if any, or 0 (zero) if
     * no value is present.
     *
     * @return hash code value of the present value or 0 if no value is present
     */
    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    /**
     * Returns a non-empty string representation of this Optional suitable for
     * debugging. The exact presentation format is unspecified and may vary
     * between implementations and versions.
     *
     * @return the string representation of this instance
     * @implSpec If a value is present the result must include its string
     * representation in the result. Empty and present Optionals must be
     * unambiguously differentiable.
     */
    override fun toString(): String {
        return if (value != null
        ) String.format("Optional[%s]", value) else "Optional.empty"
    }
}
