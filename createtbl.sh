db2 connect to COMP421
db2 -t -f createtbl.sql -z createtbl.log
db2 -td@ -vf q7_trigger_audit.sql -z q7_trigger.log