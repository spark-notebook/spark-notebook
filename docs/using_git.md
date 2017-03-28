# Documentation

## Git integration

The Spark Notebook has support for GIT version control.

In this document we will cover the [usage](#usage) and [configuration](#configuration) aspects of GIT support.


### Usage

#### Maintaining versions of your work

When GIT support is enabled, the `checkpoint` function allows the user to provide a descriptive name to the current notebook. This creates a _commit_ in GIT terminology using the given message as description. 
This _commit_ can be used at a later stage to restore a notebook to a previous state.

![Checkpoint message](../images/enterprise/checkpoint-message-highlighted.png)

In addition to manual commits, the Spark Notebook also has an automatic snapshot function that will create intermediate versions of the notebook at regular time intervals. These automatic versions can be used when work gets lost for unexpected reasons or when one would like to visit a previous version but did not create a manual commit of it.

Automatic checkpoints always have the same message "Saved file &lt;notebook-name&gt;". 
(At the moment of writing, the save interval time cannot be changed, but that will be addressed in a future version of the Spark Notebook.)

#### Restoring a previously saved version

There are two options to restore a previous version of the Spark Notebook:
* The _Revert Checkpoint_ menu entry: It lists the lastest saved checkpoints. Clicking on an entry will restore the notebook to that corresponding version. 

  ![Revert checkpoint menu](../images/enterprise/revert-checkpoint-menu.png)

* Versions side bar: If the side-bar is visible, the versions box can be used to search and restore a given version. The search function will use keywords to retrieve matching candidates. If no search is provided, all versions, in reverse chronological order, will be presented.  Clicking on an entry will restore the notebook to that corresponding version.

  ![Versions side bar](../images/enterprise/versions-side-bar.png)

Notes:
* When restoring a previous version, the current state of the Notebook will be replaced. Any unsaved work will be lost.
* Saving or checkpointing the restored version will 'promote' it as the current version.
* Restored versions will be saved by the automatic saving function, and hence promoting it to 'latest'. Therefore, restored versions can only be explored for a limited amount of time before they are persisted.

#### Deleting a Notebook

The 'delete' button will delete a notebook from the current state of version control. A current limitation is that deleted Notebooks cannot be restored from a previous version.


### Configuration

GIT support is enabled by using the `notebook.io.GitNotebookProviderConfigurator` to the configuration entry `manager.notebooks.io.provider`:

```javascript
manager {
  notebooks {
    io.provider = "notebook.io.GitNotebookProviderConfigurator"
    ...
  }
}
```

This Notebook provider takes the GIT configuration in a separate stanza:

```javascript
notebook.io.GitNotebookProviderConfigurator {
   
    local_path = ${manager.notebooks.dir}
    
    # remote = "ssh://github.com/spark-notebook/unit-test-repo.git"
    # authentication.key_file = "${HOME}/.ssh/id_rsa"
    # remote = "http://gitblit-node:8008/r/bigdata.git"
    # authentication {
    #     username: "user",
    #     password: "password"
   }
  }
```
If no remote configuration is provided, a local GIT repository will be initialized and used to store the notebooks.
By providing a valid remote and corresponding credentials, the versions of the notebooks will be pushed to said repository.

SSH and HTTPS credentials are supported and are mutually exclusive, meaning that the configuration should contain either an SSH remote URL and the corresponding `autentication.key_file` xor a HTTPS remote URL and the corresponding `authentication.username` and `authentication.password` credentials.

You can use a Github token in `authentication.password`.

### Known issues

Have in mind that Git support is still experiemental.

- if you provided wrong Git configuration, you need to **remove the local git** directory (`NOTEBOOKS_DIR`), and **restart the spark-notebook**
- the local git directory is assumed to be either not existent, or a correctly set-up git dir
- At the moment, SSH auth do not seem to work with a password protected key_file
