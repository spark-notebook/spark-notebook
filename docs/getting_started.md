# Documentation

## Getting Started with the Spark Notebook

After installing (and optionally, configuring) your Spark-Notebook package, proceed to go to the install directory  and start the server:

(*nix)
```bash
$>bin/spark-notebook.sh
```
(Windows)

```bash
C:/path/to/notebook>bin/spark-notebook.bat
```

Head to your browser of choice and access the url: http://localhost:9000
If all went well, you will see the Notebook server file browser:
![File Browser](./images/notebook_server_home.png)

You are ready to start exploring the Spark Notebook.

### The Notebook Browser


The Notebook browser consists of three tabs: Files, Running  and Cluster

#### Files Tab
This tab shows the file browser and gives quick access to the notebooks hosted in this server. The folder structure allows for a familiar hierarchical organization of the notebooks. 
The Spark Notebook comes loaded with many examples that will show in the file browser. 
These examples are the best way to get familiar with the notebook.  Feel free to explore them after this tutorial.

Each Notebook entry consists of the Notebook name and several function buttons:
![notebook-entry](./images/spark-101.png)
* Clicking on the notebook name will open the notebook in the interactive editor.
* View (read-only):  opens the notebook in read-only mode. The notebook contents will be rendered in the same state as they were last saved. This mode also allows for a "follow along" visualization: Other user can open the same notebook in interactive mode, and the _read-only_ visualization will be updated as changes happen in the interactive mode.
* Duplicate: ![Ducplicate button](/docs/images/duplicate-notebook.png) Creates a new copy the selected notebook. It will pop a dialog asking for confirmation.  
* Delete | Shutdown : This button changes mode with the state of the notebook:
  * Shutdown - ![Shutdown button](/docs/images/notebook-running-shutdown-button.png)  If the notebook is currently running, this button will display "shutdown", allowing us to shutdown the running notebook. (see the #notebook section for more details on the runtime)
  * Delete - ![Delete button](/docs/images/delete-notebook-dialog.png) a stopped notebook can be deleted from the filesystem. 

#### The Running Tab
![Running Tab](/docs/imagesrunning-tab.png) 

This tab shows a summary of the notebooks currently running in the system. Learn more about notebook resources. [TODO]
In this view, the notebook listing only show the "View (read only" and "Shutdown" buttons described above.

#### The Clusters Tab
~[Cluster Tab](/docs/images/cluster-tab.png)

This tab lets the user to create pre-defined configurations corresponding to the environment(s) that the notebook can connect to. In particular, this permits the specification of one or more Spark/Hadoop ecosystems for the Notebook to run against.


For this introductory guide, we are, we are going to explore [core/spark-101](http://localhost:9000/notebooks/core/Spark-101.snb)


notebooks use resources