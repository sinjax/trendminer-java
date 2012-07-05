Trendminer Java tools
============

Java versions of the twitter preprocesing tools written in java. As of 20/02/2012 the tools currently support java implementations of:
	- twitter tokenisation: a (slightly modified) Java version of twokeniser https://bitbucket.org/jasonbaldridge/twokenize
	- language detection: a direct port of langid.py, minus the training code (a model is provided)

In the trendminer-project directory there is a partial mirror of the openimaj:http://openimaj.org workspace for this project. Here you will see all the components
from openimaj that are currently involved with trendminer. The components which trendminer shouldn't care about are those in the core libraries of
openimaj. These are of course, still open source and simply linked remotely, if the code interests you I have detailed below how you can use maven
to get all the sourcecode you need. What I have included directly are the following projects:

	text/nlp - Low level NLP stuff you can imagine being applied to anything, not just twitter (currently contains the actual code for the tokeniser and language detection)
	web/twitter - Twitter specific code, currently contains the twitterstatus model object, some code for reading tweets from files, streams and memory and code for serialising tweets
	tools/TwitterProcessingTool - A command line tool for actually doing the processing, written in an abstract way so it can be extended to be a hadoop tool
	hadoop/tools/HadoopTwitterProcessingTool - the hadoop version of the command line tool

USMF
====
The tool has been updated to support [USMF](https://github.com/Tawlk/hyve/wiki/Unified-Social-Media-Format-(USMF)). 
The idea is that social media artefacts are now held in a uniform format which contains the salient information from many forms of social media.
This particular format is in use by the guys over at [Tawlk](http://tawlk.com/) in tools [hyve](https://github.com/Tawlk/hyve) and [kral](https://github.com/Tawlk/kral).

At the moment we only support the translation of Twitter -> USMF, but it is trivial to add more translators. With USMF our preprocessing and analysis tools can be written
with USMF in mind, and therefore deal with all forms of social media provided that a good translator is implemented.



INSTALLATION
============

Let's talk about maven.

Maven is a really amazing tool for working with java projects. It does all of the horrible work for you like jars, versions, assembling final jars and so on.
Here is a quick tutorial of how to get going with the maven project for trendminer. You can download maven itself from here: http://maven.apache.org/. It is just
a binary that needs java. An easier way to install maven is to own ubuntu (apt-get install maven2) and even EASIER is to own a mac (it is installed by default).

Once you have maven installed, lets compile the source.

Firstly, cd to the trendminer-project directory:

	cd trendminer-project

Before anything you have to make sure all the maven pom.xml files are in the right place.
The reason they are not in the right place by default is complicated and involves a weird nuance of my development environment
In future releases I hope to fix this but for now you should do:

	./preparePOM.sh

before doing anything else
from here you can do this:

	mvn install 

this is the command you need to run install everything, it also installs the source code and does all the tests. The tests can be a bit annoying to run every time you install so you can
skip those using:

	mvn install -Dmaven.test.skip=true

That is it. You now have a bunch of jars and you can include those in your projects. The jars themselves live in the .m2/repository directory of your user account. You can edit the code in your favourite editor, vim, emacs, whatever and run it using the standard java -cp command to launch them. 

However, this is a really horrible way to work and maven can help you out a lot more. My favourite way to work with java is eclipse, let's prepare the projects to load into eclipse:

	mvn eclipse:eclipse

this command prepares all the projects and tools to be imported into eclipse. Want to make sure maven downloads all the sourcecode for projects to actually in your project? No problem! Do:

	mvn eclipse:eclipse -DdownloadSources=true

Now, fire up eclipse and import the projects. You might notice that it can't find the M2_REPO. You have to make this variable in the Preferences>Java>Build Path>Classpath Variables in eclipse and make it point to ~/.m2/repository. 

That is it. From eclipse you can mess around with the code and make new additions to the tools. Please write unit tests for anything you create.

Now, let's talk about assemblies. In maven land these are self contained jars which can be treated like statically linked command line binaries. go to the directory of the command line tool TwitterProcessingTool:

	cd tools/TwitterProcessingTool

once you are here you can do:

	mvn assembly:assembly -Dmaven.test.skip=true

Done. Now in the target directory you should have a jar called: TwitterProcessingTool.jar. From here you can do this:

	java -jar TwitterProcessingTool.jar

and you should see the help for the TwitterProcessingTool. Great! You can use this tool to preprocess tweets etc.

TODO: how can I add a mode to the tool?
