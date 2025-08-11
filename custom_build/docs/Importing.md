# Importing the project
The easiest option is to [fork](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/fork-a-repo) and [clone](https://docs.github.com/en/repositories/creating-and-managing-repositories/cloning-a-repository) this repo. Or, optionally, just clone it.  
However, keep in mind that Gitub doesn't let forks of a public repo be private, so any changes you make and want to push, will be public.  
This is a good option if you plan to [build with Android Studio](AndroidStudio.md) or command-line and don't need to push your changes.  

If you plan to [build with GitHub Action](GithubActions.md), **DO NOT** create a fork, since it is required to push all your configurations and keystores to GitHub for the build process, so you want to make sure you are pushing to a PRIVATE repo.

## Creating a private import
Do **not** use GitHub's "fork" functionality, because forks cannot be made private.  
Instead, create a [new import](https://github.com/new/import) and for the source URL enter:  
`https://github.com/eluv-io/elv-wallet-android`

<details>
<summary>See screenshot</summary>
<img src="images/import-url.png" />
</details>
<br/>

Choose an owner (your organization) and their repo name, and make sure to set the visibility to private.
<details>
<summary>See screenshot</summary>
<img src="images/import-visibility.png" />
</details>
<br/>

After GitHub is done creating the import for you, clone the repo to your local machine (see [Cloning a repository](https://docs.github.com/en/repositories/creating-and-managing-repositories/cloning-a-repository)). 
