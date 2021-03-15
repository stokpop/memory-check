# Stokpop Memory Check

Read multiple memory histograms from Java and report on possible memory leaks. Sponsored by Rabobank.

# Usage

Run and provide a directory with histogram dumps and the file extension used

    java -jar memory-check-exec.jar -d /path/to/histo-dumps -e histo

Optionally a settings argument, consisting of comma separated list of 
`grow_critical,grow_minor,grow_safe,shrink,shrink_and_grow,stable`. This determines which categories to report.
Default is `-s grow_critical,grow_minor`.

For help:

    java -jar build/libs/memory-check-exec-1.1.0.jar -help
    Usage: memory-check-cli [OPTIONS]
    
    Options:
      -d, --dir TEXT         Look in this directory for heap histogram dumps.
      -e, --ext TEXT         Only process files with this extension, example:
                             'histo'
      -i, --id TEXT          Identifier for the report, example: 'test-run-1234'.
                             Include #ts# for a timestamp.
      -r, --report-dir TEXT  Full or relative path to directory for the reports,
                             example: '.' for current directory
      -c, --class-limit INT  Report only the top 'limit' classes, example: '128'.
      -b, --bytes-limit INT  Report class only when last dump has at least x
                             bytes, example: '2048'
      -s, --settings TEXT    Comma separated file with categories to report:
                             grow,shrink,unknown,stable. Default: 'grow'
      -h, --help             Show this message and exit

Example call:

    java -jar memory-check-exec.jar -d /Users/stokpop/afterburner-run-2 -e histo -i test-run-123-#ts# -r . -c 256 -b 1000 -s grow,shrink,unknown
    
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
    
    Found possible memory leaks:
    [J instances: [93733, 98323, 102994, 107584] diff: [4590, 4671, 4590] size: [72.9 MB, 76.5 MB, 80.1 MB, 83.7 MB] diff: [3.6 MB, 3.6 MB, 3.6 MB]
    [I instances: [289506, 303282, 317307, 331074] diff: [13776, 14025, 13767] size: [18.7 MB, 19.5 MB, 20.4 MB, 21.3 MB] diff: [896.7 KB, 912.8 KB, 896.3 KB]
    java.util.GregorianCalendar instances: [93717, 98307, 102978, 107568] diff: [4590, 4671, 4590] size: [10.0 MB, 10.5 MB, 11.0 MB, 11.5 MB] diff: [502.0 KB, 510.9 KB, 502.0 KB]
    sun.util.calendar.Gregorian$Date instances: [93717, 98307, 102978, 107568] diff: [4590, 4671, 4590] size: [8.6 MB, 9.0 MB, 9.4 MB, 9.8 MB] diff: [430.3 KB, 437.9 KB, 430.3 KB]
    sun.util.calendar.ZoneInfo instances: [93720, 98310, 102981, 107571] diff: [4590, 4671, 4590] size: [5.0 MB, 5.3 MB, 5.5 MB, 5.7 MB] diff: [251.0 KB, 255.4 KB, 251.0 KB]
    [Z instances: [94096, 98686, 103357, 107947] diff: [4590, 4671, 4590] size: [3.6 MB, 3.8 MB, 4.0 MB, 4.1 MB] diff: [179.3 KB, 182.5 KB, 179.3 KB]
    nl.stokpop.afterburner.domain.BigThing instances: [93690, 98280, 102951, 107541] diff: [4590, 4671, 4590] size: [2.1 MB, 2.2 MB, 2.4 MB, 2.5 MB] diff: [107.6 KB, 109.5 KB, 107.6 KB]
    [Ljava.lang.Object; instances: [22921, 23432, 23955, 24469] diff: [511, 523, 514] size: [1.2 MB, 1.2 MB, 1.3 MB, 1.3 MB] diff: [27.9 KB, 28.6 KB, 28.0 KB]
    java.util.ArrayList instances: [15666, 16176, 16704, 17217] diff: [510, 528, 513] size: [367.2 KB, 379.1 KB, 391.5 KB, 403.5 KB] diff: [12.0 KB, 12.4 KB, 12.0 KB]
    java.util.Random instances: [10413, 10923, 11442, 11952] diff: [510, 519, 510] size: [325.4 KB, 341.3 KB, 357.6 KB, 373.5 KB] diff: [15.9 KB, 16.2 KB, 15.9 KB]
    java.util.concurrent.atomic.AtomicLong instances: [10475, 10985, 11504, 12014] diff: [510, 519, 510] size: [245.5 KB, 257.5 KB, 269.6 KB, 281.6 KB] diff: [12.0 KB, 12.2 KB, 12.0 KB]
    nl.stokpop.afterburner.domain.BigFatBastard instances: [10410, 10920, 11439, 11949] diff: [510, 519, 510] size: [244.0 KB, 255.9 KB, 268.1 KB, 280.1 KB] diff: [12.0 KB, 12.2 KB, 12.0 KB]
    
# Build executable jar

If you want to build an executable jar yourself use:

    ./gradlew clean fatJar
