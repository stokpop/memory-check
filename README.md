# Stokpop Memory Check

Reads multiple memory histograms from standard Java histogram dumps and report on possible memory leaks. 

Sponsored by Rabobank.

# Quick start

Point to a directory with histogram-dumps and the file extension used for the histogram files.
In this example the files are called `memory-dump-2021-03-03T00-30-52.435.histo`. 
How to create multiple histograms over time is described below.

    java -jar memory-check-exec.jar -d /path/to/histo-dumps -e histo

![summary of findings](images/analysis-summary-1.png?raw=true "bytes-diff graph")

The analysis result categories are:

| analysis result | count | icon | description                                                           |
|-----------------|-------|------|-----------------------------------------------------------------------|
| GROW_CRITICAL   | 61    | ▲    | critical growth detected (above 'maximum allowed growth percentage')  |
| GROW_MINOR      | 3     | ⇧    | minor growth detected (below 'maximum allowed growth percentage')     |
| GROW_SAFE       | 4     | ↑    | growth detected in 'safe list' of known growing classes               |
| GROW_HICK_UPS   | 28    | ⥣    | growth with hick-ups (less than 'minimum growth points percentage')   |
| SHRINK_AND_GROW | 320   | ↕    | both shrink and growth detected                                       |
| STABLE          | 533   | ↔    | all histograms show same number of objects                            |
| SHRINK          | 2     | ↓    | opposite of growth: only shrinks detected                             |
| UNKNOWN         | 0     | ?    | no matching analysis result                                           |

# Concepts

Histogram dumps contain lines for all classes in the jvm with the number of instances
and the total number of bytes used by these classes. You can specify to list the
_live_ objects only. Because we are interested in memory leaks, we do not want the non-live objects that
can be collected by a garbage collect.

Contents of a histogram dump file, we only show the 'top-10':

```text
 num     #instances         #bytes  class name (module)
-------------------------------------------------------
   1:        181128       21229800  [B (java.base@11.0.10)
   2:        188244        6023808  java.util.concurrent.ConcurrentHashMap$Node (java.base@11.0.10)
   3:        162431        3898344  java.lang.String (java.base@11.0.10)
   4:         40944        3603072  java.lang.reflect.Method (java.base@11.0.10)
   5:         10342        3054664  [Ljava.util.concurrent.ConcurrentHashMap$Node; (java.base@11.0.10)
   6:         23108        2931360  java.lang.Class (java.base@11.0.10)
   7:         59338        2848224  org.aspectj.weaver.reflect.ShadowMatchImpl
   8:         15952        2171032  [I (java.base@11.0.10)
   9:         59338        1898816  org.aspectj.weaver.patterns.ExposedState
  10:         36916        1790504  [Ljava.lang.Object; (java.base@11.0.10)
  ...
```

On number 1 is the `[B` class name, which are byte arrays. See this [overview of Java VM Type Signatures](https://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/types.html#wp16432).
Byte arrays are for instance backing arrays of Strings, which are on place 3. On spot 10 we see arrays
of Objects: `[Ljava.lang.Object;`. 

The number of bytes is the bytes taken by the instances itself, not by referent instances. For instance,
the String classes take 3.898.344 bytes for 162.431 instances, which is basically 24 bytes per instance.

# Analysis

All instances and bytes for a class name is matched to the instances and bytes in all histograms, 
ordered on time. So a timestamp in the histogram dump names is essential.

Next, the possible growth is checked over time, looking at the number of bytes per class.

The overall analysis shows classification of all classes that have more bytes in last
histogram than specified via `--bytes-limit`.

The detailed analysis and graphs only report the specified classifications 
(e.g. only grow_critical and grow_minor) and only specified top number of classes 
(with `--class-limit` option).

## growth

If we find an increase in bytes in _all_ histograms, the class is said to grow.
_This might be a memory leak._ 

If the increase in *bytes*, from the first dump to the last dump
is above a certain percentage threshold, the growth is marked as *critical*. Say 5 percent.
If it is below this threshold, it is considered a *minor* growth. 
Use the `--max-growth-percentage` option.

## growth hick ups

Some classes do not grow on every histogram dump. For a load test with continuous load
we expect most (but not all!) leaks to grow continuously with the constant flow of requests.
For dumps of production systems this can of course be different, depending on incoming
requests over time.

We classify GROWTH_HICK_UPS when from all histograms we see only growth, but not in all histograms.
Use the `--min-growth-points-percentage` to specify the percentage of histograms to should
show growth. For instance, when set to 50% and with 11 dumps, at least 5 differences between
dumps should show growth to be GROW_CRITICAL or GROW_MINOR. Otherwise it is GROW_HICK_UPS.

Note: no check on bytes growth is done on GROW_HICK_UPS to determine CRITICAL or MINOR.

## shrink and grow

Classes that show both shrinks and grows in bytes are classified as SHRINK_AND_GROW.

Note: no check on bytes growth is done on SHRINK_AND_GROW to determine CRITICAL or MINOR.

## stable

Classes that show same size in bytes in all histograms are classified as STABLE.

## unknown

When last histogram does not contain the class, so we do not know the final number of
bytes, the classification in UNKNOWN.

Also when not enough data is available the classification is UNKNOWN. Some dynamicly 
named classes might for instance only be present in one dump (with instances and bytes).

## safe-list

Some classes that show growth might be considered false positives. Or are recognized to grow
always during load tests or production run, but cannot be fixed after investigation. 

We found some tomcat classes to grow over time during Spring Boot application load tests and
we could not find a good way to avoid or solve this growth. It seems to get more stable over
time as well. After investigation, we `allow` these classes to grow but still want to see
them in the reports. 

For these cases the class can be put on the _safe-list_. The following happens:
* the classification is set to `GROW_SAFE` instead of `GROW_CRITICAL` or `GROW_MINOR` when growth is detected
* in the reports the class names are prefixed by `(SL)` for Safe-List

Classes can be added to the safe-list via comma separated command line or via a safe-list file.
A wildcard can be used with `*`, e.g. `java.lang.*` or `*Connection*`

## watch-list

Classes that need special attention can be put on the _watch-list_. The following happens:

* even when the number of bytes in the last histogram is below the minimum threshold for including
  the class in the report details, the class is shown
    
* even when the maximum number of classes to report in the details is reached, the classes on
the watch list are still included

* in the reports the class names are prefixed by `(WL)` for Watch-List

Classes can be added to the watch-list via comma separated command line or via a safe-list file.

Example call:

```text
java -jar memory-check-exec-1.2.1.jar \
  --dir /path/to/jvm-histograms/ \
  --ext histo \
  --id duration-test-123-#ts# \
  --report-dir my-reports \
  --class-limit 42 \
  --bytes-limit 100kb \
  --max-growth-percentage 10 \
  --min-growth-points-percentage 5 \
  --categories grow_critical,grow_minor,grow_safe \
  --safe-list java.lang.String \
  --watch-list java.lang.ThreadLocal,nl.stokpop.*
```

# Use cases

* Automatically check for possible memory leaks after continuous load tests.
* Create some dumps during a troubleshoot and run an analysis.
* Create some dumps to do a memory usage sanity check on applications it test or production.
* Find memory behaviour of specific classes.

To get to the proper level of reporting, use the available thresholds and settings.

If interested in classes that only grow, no matter how much:
```text
  --class-limit 1000 \
  --bytes-limit 0 \
  --max-growth-percentage 0 \
  --min-growth-points-percentage 100 \
  --categories grow_critical,grow_minor \
```
Show all for only the classes on watch-list, e.g. all classes with Connection in the name:
```text
  --class-limit 0 \
  --bytes-limit 0 \
  --max-growth-percentage 0 \
  --min-growth-points-percentage 0 \
  --categories all \
  --white-list *Connection*
```

Will show all Connection related classes of hibernate and apache:

![screen shot connections graph](images/apache-hibernate-connections-bytes-graph.png?raw=true "bytes-diff graph")

    
# Reports

The memory-check will create a text output on standard out and a `json` and `html` report.
Use the `id` option to give the reports a custom name. Use the `report-dir` option to save 
it in another location.

The html report show graphs of bytes and instances in use over time. 

In the diff (difference) charts you can find leaks if the graph line stays above the zero line, 
meaning that objects are created but not removed.

![screen shot of bytes-diff graph](images/bytes-diff-mem-leak-example-highlites.png?raw=true "bytes-diff graph")

## Report used files (optional)

You can include the list of files that were used to build the report (all processed histogram files and optional safe/watch list files) by adding the `--report-used-files` flag.

- HTML: a "Used files" section is added near the summary, listing the file names.
- JSON: a `usedFiles` object is added at the root with:
  - `histogramFiles`: array of absolute paths
  - `safeListFile`: optional absolute path
  - `watchListFile`: optional absolute path

Example:

```json
{
  "usedFiles": {
    "histogramFiles": [
      "/path/to/histos/dump-1.histo",
      "/path/to/histos/dump-2.histo"
    ],
    "safeListFile": "/path/to/safe-list.txt",
    "watchListFile": "/path/to/watch-list.txt"
  }
}
```

To enable in CLI:

```text
--report-used-files
```

# Generate histogram dump

Use the following command to dump the live objects of a java process.

    jmap -histo:live $JAVA_PID > memory-dump-$(date +%Y-%m-%dT%H-%M-%S).histo
    
Generate 4 dumps with 10 seconds apart for application-name:

    ./tools/create-dumps.sh application-name 4 10000   
    
Generate histogram via jmx:

    java -cp memory-check-exec.jar nl.stokpop.jmx.FetchHistogramKt localhost 5000 > memory-dump-$(date +%Y-%m-%dT%H-%M-%S).histo
    
Enable jmx on your java process by adding these jvm options:

    -Dcom.sun.management.jmxremote.port=5000
    -Dcom.sun.management.jmxremote.ssl=false
    -Dcom.sun.management.jmxremote.authenticate=false

Make sure there is a timestamp in the dump filename that memory-check can use to determine the 
time of the dump. If not found in the filename it will try to use the creation date file attribute,
but that might not always be correct, for example after a file copy. Use the following format:

    yyyy-MM-ddTHH-mm-ss(.SSS)

Optionally you can also add milliseconds/nanoseconds for better precision. Example filename:

    memory-dump-2020-06-17T17-52-32.433463.histo

# Histogram file names

Memory-check first checks the filename to contain a ISO date and uses that as a timestamp
for the histogram dump. The filename should contain an ISO formatted date like: 
`2020-06-17T22-25-38.960921`
If not found, uses file creation date or file last modified as last resort.
Be very careful that these times are correct! It is adviced to always use timestamps
in the filenames.

# Usage

```text
Usage: memory-check-cli [<options>]

Options:
  -d, --dir=<text>                               Look in this directory for heap histogram dumps.
  -e, --ext=<text>                               Only process files with this extension, example: 'histo'
  -i, --id=<text>                                Identifier for the report, example: 'test-run-1234'. Include #ts# for a timestamp.
  -r, --report-dir=<text>                        Full or relative path to directory for the reports, example: '.' for current directory
  -c, --class-limit=<int>                        Report only the top 'limit' classes, example: '128'.
  -b, --bytes-limit=<text>                       Report class only when last dump has at least x bytes, example: '2048' or '2KB'
  -cat, --categories=<text>                      Comma separated file with categories to report: 'grow_critical, grow_minor, grow_safe,
                                                 grow_hick_ups, shrink_and_grow, shrink, stable, unknown'. Or 'all'. Default:
                                                 'grow_critical,grow_minor'
  -mgp, --max-growth-percentage=<float>          Maximum allowed growth in percentage before reporting a critical growth. Default: 5.0
  -mgpp, --min-growth-points-percentage=<float>  Minimum percentage of growth points to be considered growth. Default: 50.0
  -sl, --safe-list=<text>                        Comma separated list of fully qualified classnames that are 'safe to growth'. The asterisk (*)
                                                 can be used as wildcard. Default: ""
  -wl, --watch-list=<text>                       Comma separated list of fully qualified classnames that are 'always watched' irrelevant of other
                                                 settings. The asterisk (*) can be used as wildcard. Default: ""
  -slf, --safe-list-file=<path>                  The safe list file. Should contain one fully qualified classname per line.
  -wlf, --watch-list-file=<path>                 The safe list file. Should contain one fully qualified classname per line.
  -ruf, --report-used-files                      Include the list of used files (histograms, safe/watch list files) in the JSON and HTML reports.
  -h, --help                                     Show this message and exit
```

# Build executable jar

If you want to build an executable jar yourself use:

    ./gradlew clean fatJar