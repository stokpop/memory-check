# Stokpop Memory Check

Read multiple memory histograms from Java and report on possible memory leaks. Sponsored by Rabobank.

# Usage

Run and provide a directory with histogram dumps and the file extension used

    java -jar memory-check-exec.jar -d /path/to/histo-dumps -e histo

Optionally a settings argument, consisting of comma separated list of 
`grow_critical,grow_minor,grow_safe,shrink,shrink_and_grow,stable`. This determines which categories to report.
Default is `-s grow_critical,grow_minor`.

For help:

```text
java -jar memory-check-exec-1.2.0.jar -h
Usage: memory-check-cli [OPTIONS]

Options:
  -d, --dir TEXT                   Look in this directory for heap histogram
                                   dumps.
  -e, --ext TEXT                   Only process files with this extension,
                                   example: 'histo'
  -i, --id TEXT                    Identifier for the report, example:
                                   'test-run-1234'. Include #ts# for a
                                   timestamp.
  -r, --report-dir TEXT            Full or relative path to directory for the
                                   reports, example: '.' for current directory
  -c, --class-limit INT            Report only the top 'limit' classes,
                                   example: '128'.
  -b, --bytes-limit INT            Report class only when last dump has at
                                   least x bytes, example: '2048'
  -s, --settings TEXT              Comma separated file with categories to
                                   report:
                                   grow_critical,grow_minor,grow_safe,shrink,unknown,stable.
                                   Default: 'grow_critical,grow_minor'
  -mgp, --max-growth-percentage FLOAT
                                   Maximum allowed growth in percentage before
                                   reporting a critical growth. Default: 5.0
  -mgpp, --min-growth-points-percentage FLOAT
                                   Minimum percentage of growth points to be
                                   considered growth. Default: 50.0
  -sl, --safe-list TEXT            Comma separated list of fully qualified
                                   classnames that are 'safe to growth'. The
                                   asterisk (*) can be used as wildcard.
                                   Default: ""
  -wl, --watch-list TEXT           Comma separated list of fully qualified
                                   classnames that are 'always watched'
                                   irrelevant of other settings. The asterisk
                                   (*) can be used as wildcard. Default: ""
  -slf, --safe-list-file PATH      The safe list file. Should contain one
                                   fully qualified classname per line.
  -wlf, --watch-list-file PATH     The safe list file. Should contain one
                                   fully qualified classname per line.
  -h, --help                       Show this message and exit
```

Example call:

```text
java -jar memory-check-exec-1.2.0.jar \
  --dir /path/to/jvm-histograms/ \
  --ext histo \
  --id duration-test-123-#ts# \
  --report-dir my-reports \
  --class-limit 42 \
  --bytes-limit 100kb \
  --max-growth-percentage 10 \
  --min-growth-points-percentage 5 \
  --settings grow_critical,grow_minor,grow_safe \
  --safe-list safe-list.txt \
  --watch-list watch-list.txt
```
    
# Reports

The memory-check will create a text output on standard out and a json and html report.
Use the `id` option to give the reports a custom name. Use the `report-dir` option to save 
it in another location.

The html report show graphs of bytes and instances in use over time. 

In the diff (difference) charts you can find leaks if the graph line stays above the zero line, 
meaning that objects are created but not removed.

![screen shot of bytes-diff graph](images/bytes-diff-mem-leak-example-highlites.png?raw=true "bytes-diff graph")

# Generate histogram dump

Use the following command to dump the live objects of a java process.

    jmap -histo:live $JAVA_PID > memory-dump-$(date +%Y-%m-%dT%H:%M:%S).histo
    
Generate 4 dumps with 10 seconds apart for application-name:

    ./tools/create-dumps.sh application-name 4 10000   
    
Generate histogram via jmx:

    java -cp memory-check-exec.jar nl.stokpop.jmx.FetchHistogramKt localhost 5000 > memory-dump-$(date +%Y-%m-%dT%H:%M:%S).histo
    
Enable jmx on your java process by adding these jvm options:

    -Dcom.sun.management.jmxremote.port=5000
    -Dcom.sun.management.jmxremote.ssl=false
    -Dcom.sun.management.jmxremote.authenticate=false

Make sure there is a timestamp in the dump filename that memory-check can use to determine the 
time of the dump. If not found in the filename it will try to use the creation date file attribute,
but that might not always be correct, for example after a file copy. Use the following format:

    yyyy-MM-ddTHH:mm:ss(.SSS)

Optionally you can also add milliseconds/nanoseconds for better precision. Example filename:

    memory-dump-2020-06-17T17:52:32.433463.histo
        
# Example output

    Number of GROW 12
    Number of STABLE 7658
    Number of SHRINK 0
    Number of UNKNOWN 693

# Build executable jar

If you want to build an executable jar yourself use:

    ./gradlew clean fatJar
