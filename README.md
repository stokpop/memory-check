# Stokpop Memory Check

Read multiple memory histograms from Java and report on possible memory leaks.

# Usage

Run and provide a directory with histogram dumps and the file extention used

    java -jar memory-check.jar /path/to/histo-dumps histo
    
# Generate histgram dump

Use the following command to dump the live objects of a java process.

    jmap -histo:live $JAVA_PID > memory-dump-$(date +%Y%m%d.%H%M%S).histo
    
Generate multiple dumps with some time apart.

# Example output

    Number of GROW 1
    Number of STABLE 7658
    Number of SHRINK 0
    Number of UNKNOWN 693
    
    Found possible memory leaks:
    sun.nio.cs.UTF_8$Encoder instances: [70, 112, 210, 211, 277, 279, 283, 286, 287] size: [3.8 KB, 6.1 KB, 11.5 KB, 11.5 KB, 15.1 KB, 15.3 KB, 15.5 KB, 15.6 KB, 15.7 KB]
    
