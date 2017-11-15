# Documentation

## Exploring the Notebook interface

For this introductory guide we are going to use [core/spark-101](http://localhost:9001/notebooks/core/Spark-101.snb.ipynb)
*(for the links in this page to work, we assume that the Spark Notebook is running on localhost on the default port (9001))*

Click on the folder [core](http://localhost:9001/tree/core) and then click on the [spark-101](http://localhost:9001/notebooks/core/Spark-101.snb.ipynb) notebook.

The Spark Notebook main abstraction is a _cell_. A cell can contain text, in the form of Markdown or Scala/Spark code.

![markdown cell](./images/markdown-cell.png)

Cells are editable. To enter edit mode, click on them or press enter when the cell is highlighted. 

![code cell - edit mode](./images/code-cell.png)

Once you've edited a cell, pressing **&lt;Ctrl&gt;-Enter** will execute the cell and display the result in the output field within the cell, right below the edit area.

![code cell - executed](./images/code-cell-executed.png)

Once executed, the definitions within the cell become part of the execution context. Already defined elements can then be used in other cells, in order to interactively and gradually build up the desired logic.

![code cell - uses previous definition ](./images/code-cell-2.png)

 We can make use of the full spectrum of the Scala programming language: `imports`, `classes`,  `objects`, `values`, `variables`, `functions`, ..._ can be defined and used within the context of the notebook.

### Built-in Spark

The Spark Notebook is all about Spark. An implicit Spark context is built from the configuration and made directly available. With the Spark Notebook, you can start using Spark right away.

![built-in Spark Context](./images/built-in-spark-context.png)


### Automatic visualizations

When data collections, like arrays, lists or sets, are returned from a cell, the Spark Notebook will offer automatic visualizations. The options will depend of the specific type of the data. For example, `case classes` will be displayed as text, tuples of 2 elements `(x,y)` will have chart representations like scatter plot, line charts or bar charts. Highly customized visualizations are possible using provided widgets.

#### Table
![table](./images/auto-viz-table.png)

#### Scatter Plot
![scatter plot](./images/auto-viz-scatter-plot.png)

#### Line Chart
![Line Chart](./images/auto-viz-line-chart.png)

#### Bar Chart
![Bar Chart](./images/auto-viz-bar-chart.png)

#### Pivot Table
![Pivot Table](./images/auto-viz-pivot-table.png)


### Explore ...

Go through the complete notebook, changing and executing the cells. Experimentation is the best way to familiarize yourself with the notebook.

***

## A Note About Immutability

 The Scala Programming Language, as part of the family of functional programming languages, promotes immutability as an important programming principle. Furthermore, when programming for distributed computing, immutability plays a key role to achieve safe and consistent results. 
 The Spark Notebook fully supports the immutable principles of Scala. Although it's possible to re-execute a cell and update the values of `objects`, what we are doing is redefining those objects in the same context. __This should not be confused with value reassignment__ which will be safely rejected by the Scala compiler.

 Due to some limitations of the underlying REPL interpreter, assignments of different values to the same symbol in different cells will result in that symbol being redefined. Uniqueness of value assignments are only supported within the same cell.

![redefinition of values](./images/redefinition-of-values.png)
