# svc-graphite
A program to send performance data of [IBM SVC (SAN Volume Controller )](http://en.wikipedia.org/wiki/IBM_SAN_Volume_Controller) and SVC software-based disk arrays ([IBM Storwize](http://en.wikipedia.org/wiki/IBM_Storwize_family) V7000, V3700, V3500) to [Graphite](http://graphite.readthedocs.org/en/latest/).

## Installation
Download latest release from https://github.com/ivoronin/svc-graphite/releases
### Enabling performance data collection

```sh
IBM_2076:V7000_1:superuser> startstats -interval 5
IBM_2076:V7000_1:superuser> lssystem
...
statistics_status on
statistics_frequency 5
...
```

## Usage
```sh
scp -r superuser@svc-cluster:/dumps/iostats iostats
java -jar svc-graphite.jar -H 172.20.56.78 -P 2003 iostats/*
```

It should be pretty easy to write a shell script to run in a cron every few minutes.

## Options
```
 Switches               Default    Desc                            
 --------               -------    ----                            
 -H, --host             127.0.0.1  Graphite server host or address 
 -P, --port             2003       Graphite server port            
 -h, --no-help, --help  false      Show help           
```

### Bugs
- High startup time
- java.io.IOException (Operation not permitted) on Linux: disable iptables and unload all iptables-related (ipt\_\*, nf\_\*, xt\_\*) modules.

## Useful links
- [Overview of SVC V5.1.0 Performance Statistics](http://www-01.ibm.com/support/docview.wss?uid=ssg1S1003597)

## License

Copyright © 2013 Ilya Voronin

Distributed under the Eclipse Public License either version 1.0.