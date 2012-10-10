OVERVIEW
============

This tool was created for efficient text processing and analysis of social media text. It works in an online setting or over batches of data. It has a pipeline architecture, with the user having control over the modules that he wishes to run, each module adding to the input. 
 
The tool has 2 variants:
- as a single node tool (i.e. TrendminerTool.jar)
- as a distributed tool that runs in the Hadoop Map-Reduce framework (i.e. HadoopTrendminerTool.jar)
 
Note: If you do not have a cluster set up for running Hadoop or want to test the tool first before running on a cluster, you should use the first variant of the tool.


QUICK START
===========

### Example 1. Running with input from the streaming API

	curl -u$TWUSER:$TWPASS https://stream.twitter.com/1/statuses/sample.json | java -jar TrendminerTool.jar -m TOKENISE -m LANG_ID -q

### Example 2. Processing a tweet file

	java -jar TrendminerTool.jar -m TOKENISE -m LANG_ID -m PORTER_STEM -i $TWFILE -o $TWFILEPROC -q

### Example 3. Processing in Map-Reduce

	hadoop jar HadoopTrendminerTool.jar -i $HDFS-INPUT -o $HDFS-OUTPUT -m TOKENISE -m LANG_ID -ri
 
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
 
Slides about the project:
- [ICWSM, June 2012](http://www.dcs.shef.ac.uk/~daniel/trendminer+ramss+2012+slides.pdf)
- [INQUEST, September 2012](http://www.dcs.shef.ac.uk/~daniel/inquest+2012+slides.pdf)
 
Paper describing the architecture and the first implemented modules:
Trendminer: An Architecture for Real Time Analysis of Social Media Text
Daniel Preotiuc-Pietro, Sina Samangooei, Trevor Cohn, Nicholas Gibbins, Mahesan Niranjan
[Workshop on Real-Time Analysis and Mining of Social Streams (RAMSS), ICWSM 2012](http://www.dcs.shef.ac.uk/~daniel/trendminer+ramss+2012.pdf)
 
OPTIONS
=======
 
When ran with no options, help information is displayed.
e.g. java -jar TrendminerTool.jar

## MODES
 
Modes are preprocessing steps that are applied in succession to the input.
- when a mode is ran over an item (e.g. tweet), the output of the analysis is held in a separate 'analysis' data field and specific keys are added to this analysis construct like defined by each of the mode's descriptions
- you can specify multiple -m options
- no given mode is ever applied multiple times
- the order of running the modes is important
- some dependencies exist and are noted in the mode description below
- this is the only compulsory option that the tool needs
 
## INPUT
 
We assume that the input files are one item/line in JSON format.
 
The tool takes input in 3 ways:

1. stdio: Input from the standard input (the default setting)
	-i -

2. single file: Input is taken from a single file
	-i filename

3. multiple files: A single file is given with file names, one per line. Each file is treated as a source of tweets.
	-if filecontainingfilenames
 
	--n-tweets N

By default, every line in all inputs is analysed unless the --n-tweets N option is specified in which case N tweets are analysed (including across multiple files)
 
	-it

Specifies the input format schema.
TWITTER 
USMF (Unified Social Media Format) is a data format that generalizes data extracted from social networks.
It was devised by Tawlk and is presented here: https://github.com/Tawlk/hyve/wiki/Unified-Social-Media-Format-(USMF) (default setting)
 
## OUTPUT
 
The tool can output to stdout (default setting) or to a file. The latter is activated with the flag -o filename 
 
The output can have multiple modes:
1. Full mode: outputs the entire original JSON with the analysis appended as a special field (default mode)
	
	-om APPEND

2. Partial mode: selective components of the original input are maintained plus the analysis. By default these selective components are the “id” and “create_data” date.

	-om CONDENSED and the components to maintain can be selected using -te component1 -te component2 etc.

3. Analysis mode: in this mode only the analysis data is outputted, all other input information is supressed.
	
	-om ANALYSIS
 
Obs: if you have hdfs installed, we observed that the most reliable way to specify the input/output is to specify the full path e.g. hdfs://localhost:8020/user/...
 
	-ot 
Specifies the output format schema. Options as -it (described above)
 
## FILTERS
 
 	--post-filter (-pof) [GEO | LANG | GREP | DATE | RANDOM | IN_REPLY_TO] Define filters. Applied after other processing.
 	
 	--pre-filter (-prf) [GEO | LANG | GREP | DATE | RANDOM | IN_REPLY_TO] Define filters. Applied before other processing.
 
## OTHER
 
	-rm
If the output file exists, remove it
 
	-ri
The return immediately flag returns control to the system after sending the Map-Reduce job. Otherwise, the system waits for the Map-Reduce job to finish before running the following command.
 
	--encoding
The encoding of the data is assumed to be UTF-8 unless specified otherwise using this option 
 
	-t N
time before skip. N seconds to wait before skipping an entry
 
	-q 
quiet
	
	-v 
verbose
 
## SUPPORT FOR LZO
 
- the tool can take lzo input and output
- this allows you to use compressed input/output while still processing in Map-Reduce
- for the libraries that allow you to use lzo with Hadoop you should refer to: https://github.com/toddlipcon/hadoop-lzo 
- before running the tool in Map-Reduce make sure that you have first indexed the files 
 
## DESCRIBING THE MODES
 
### Tokenization
	Usage:          -m TOKENISE
	Description:   splits the input text into lists of tokens
	Method:        our own tokenization tool that adapts to social media/Twitter conventions
	Dependencies: - 
	Output:
	'tokens' field with 3 subfields:
	  'all' - a list of all tokens 
	  'unprotected' - a list of all word tokens
	  'protected' - a list of all tokens that are not words (e.g. hashtags, punctuation, emoticons, url's)
 
### Language identification
	Usage:          -m LANG_ID
	Description:   automatic language identification from the text of the input
	Method:        using an implementation of the langid.py tool with the default trained model (97 languages)
	More on this in 'langid.py: An Off-the-shelf Language Identiﬁcation Tool' - Marco Lui, Timothy Baldwin
	Dependecies: -m TOKENISE (takes the input from the 'tokens'->'protected' field)
	Output:
	'langid' field with 2 subfields:
	   'confidence' - the confidence score for the prediction
	   'language' - the 2 letter code for the detected language 
 
### Stemming
	Usage:           -m PORTER_STEM
	Description:     reduces each token in the input text to it's stem
	Method:          stemming performed on the text with the Porter Stemmer (only for english texts)
	Dependencies: -m TOKENISE (takes the input from the 'tokens'->'all' field)
	Output:           'stemmed' - a list with the stemmed tokens 
 
HOW TO ADD A MODE
=================
 
...
 
FUTURE MODULES
==============
 
Location detection
POS Tagging
Named Entity Recognition
Normalizer
Spam Detection
User influence


