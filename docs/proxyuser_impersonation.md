Secure Hadoop+YARN clusters & `proxy-user` impersonation
----

If spark-notebook is used by multiple users,
 forwarding of the authenticated username is available via user impersonation 
 (just like `--proxy-user` in `spark-submit`; see [Spark Authentication @ Cloudera](https://www.cloudera.com/documentation/enterprise/5-5-x/topics/sg_spark_auth.html#concept_bvc_pcy_dt)) 
. This is available for YARN clusters only.
 

## Setting-up Spark notebook

Add this to `conf/application.conf` to enable forwarding:

```
# Set if the currently logged user which started a notebook kernel be passed to Hadoop/Spark?
# This is done via impersonation (like in: spark-submit --proxy-user):
# - the spark-notebook server is run by a single "super-user", which is impersonates itself as another user.
# - `kinit superuser` needs to be run periodically
notebook.hadoop-auth.proxyuser-impersonate = "true"
```

The setup would look like this:
- `spark-notebook` server is run by user `sparknotebook` (which has `proxyuser` privillege)
  * once `proxyuser-impersonate` enabled, notebooks would run in name of another user, the one who started the notebook's kernel/REPL.
- one would periodically run `kinit` from the same `sparknotebook` user, to make the Kerberos credentials of the base-user available so it could access the secure cluster
  * e.g. add this command to cron: `kinit -V -k -t /some-path/spark.headless.keytab -r 7d spark@somerealm.com`


## Seting up Secure Hadoop/Spark @ YARN

### Setup secure YARN
### Enable impersonation for `sparknotebook` user

  Add something like this to `core-site.xml` (likely you want to explicitly list the allowed users/groups/hosts):
  ```xml
     <property>
       <name>hadoop.proxyuser.sparknotebook.hosts</name>
       <value>*</value>
     </property>
     <property>
       <name>hadoop.proxyuser.sparknotebook.users</name>
       <value>*</value>
     </property>
     <property>
       <name>hadoop.proxyuser.sparknotebook.groups</name>
       <value>*</value>
     </property>
  ```
### Setup Spark Notebook for YARN.

See [`Secured YARN clusters` section here](./clusters_clouds.md) for a few notes on 
how to set up a spark-notebook on a secured YARN cluster.

```bash
# useful when debugging (do not use in production)
# print extra info about current Kerberos user/errors
export HADOOP_JAAS_DEBUG=true
```

### Notes
P.S. Hadoop authentication via keytab (i.e. `--keytab` & `--pricipal` in `spark-submit`) is not supported as it works only in `YARN-cluster` mode, which is problematic in REPL (and even `spark-shell` don't support it).   