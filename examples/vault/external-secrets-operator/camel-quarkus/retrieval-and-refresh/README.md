## AWS Secrets Manager Vault on OCP through External Secrets Operator infrastructure

In this sample you'll use the Kubernetes Secrets properties from Camel in combination with External Secrets Operator AWS Secrets Manager Provider

## Preparing the project

We'll connect to the `ext-secrets-camel-sql-refresh` project and check the installation status. To change project, open a terminal tab and type the following command:

```
oc new-project ext-secrets-camel-sql-refresh
```

## Setting up Database

This example uses a PostgreSQL database. We want to install it on the project `camel-transformations`. We can go to the OpenShift 4.x WebConsole page, use the OperatorHub menu item on the left hand side menu and use it to find and install "Crunchy Postgres for Kubernetes". This will install the operator and may take a couple of minutes to install.

Once the operator is installed, we can create a new database using

```
oc create -f ./infra/postgres.yaml
```

We connect to the database pod to create a table and add data to be extracted later.

```
oc rsh $(oc get pods -l postgres-operator.crunchydata.com/role=master -o name)
```

```
psql -U postgres test \
-c "CREATE TABLE test (data TEXT PRIMARY KEY);
INSERT INTO test(data) VALUES ('hello'), ('world');
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgresadmin;"
```

Now let's change the password.

```
psql -U postgres -c "ALTER USER postgresadmin PASSWORD 'masteradmin1234*';"
```

```
exit
```

Now we need to create the secret payload. Open the file postgresCreds.json and place the related exported value into the related field. You should have something like:

```
{
  "username": "user",
  "password": "pwd",
  "host": "postgres-primary.ext-secrets-camel-sql-refresh.svc"
}
```

Now create the secret with the AWS CLI or with the console:

```
aws secretsmanager create-secret --name authsecdb --secret-string file://postgresCreds.json
```

This complete the Database setup.

## Externalize secrets into External Secret operator infra

In your namespace install the External Secret operator with latest version.

Now run the following:

```
oc apply -f ./infra/operatorConfig.yaml -n ext-secrets-camel-sql-refresh
```

This should install your operator correctly for OCP.

Now run the following list of commands and substitute the KEYID and SECRETKEY with your AWS credentials

```
echo -n 'KEYID' > ./access-key
echo -n 'SECRETKEY' > ./secret-access-key
oc create secret generic awssm-secret --from-file=./access-key --from-file=./secret-access-key -n ext-secrets-camel-sql-refresh
```

Once everything is done you can create your secret

```
oc apply -f ./infra/basic-secret-store.yaml -n ext-secrets-camel-sql-refresh
```

and create the secret

```
oc apply -f ./infra/basic-external-secret.yaml -n ext-secrets-camel-sql-refresh
```

To be sure everything is fine you should find a secret named authsecdb in your secrets for the namespace ext-secrets-camel-sql-refresh.

Now we need to give application the ability to read secrets from the cluster

```
oc create clusterrole secretadmin --verb=get --verb=list --resource=secret --namespace=ext-secrets-camel-sql-refresh
```

or alternatively you can run

```
oc apply -f ./infra/secretpermission.yaml
```

## Deploy to OCP

Once the process complete

```
./mvnw install -Dquarkus.openshift.deploy=true
```

Once everything is complete you should be able to access the logs with the following command:

```
Starting the Java application using /opt/jboss/container/java/run/run-java.sh ...
INFO exec -a "java" java -XX:MaxRAMPercentage=80.0 -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -cp "." -jar /deployments/quarkus-run.jar 
INFO running in /deployments
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2024-08-27 10:53:15,115 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) Bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2024-08-27 10:53:15,118 INFO  [org.apa.cam.mai.MainSupport] (main) Apache Camel (Main) 4.6.0 is starting
2024-08-27 10:53:15,260 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Auto-configuration summary
2024-08-27 10:53:15,261 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [MicroProfilePropertiesSource] camel.main.routesIncludePattern=camel/sql-to-log.camel.yaml
2024-08-27 10:53:15,261 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [MicroProfilePropertiesSource] camel.vault.gcp.projectId=<project_id>
2024-08-27 10:53:15,442 INFO  [org.apa.cam.com.kub.pro.BasePropertiesFunction] (main) KubernetesClient using masterUrl: https://172.21.0.1:443/ with namespace: ext-secrets-camel-sql-refresh
2024-08-27 10:53:16,214 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 4.6.0 (camel-1) is starting
2024-08-27 10:53:16,240 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Property-placeholders summary
2024-08-27 10:53:16,240 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] query=select * from test;
2024-08-27 10:53:16,241 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] dsBean=dsBean-1
2024-08-27 10:53:16,241 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] delay=120000
2024-08-27 10:53:16,241 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] password=xxxxxx
2024-08-27 10:53:16,241 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] serverName=postgres-primary.ext-secrets-camel-sql-refresh.svc
2024-08-27 10:53:16,242 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] databaseName=test
2024-08-27 10:53:16,242 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] username=xxxxxx
2024-08-27 10:53:16,244 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Routes startup (total:1 started:1 kamelets:1)
2024-08-27 10:53:16,244 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started route1 (kamelet://postgresql-source)
2024-08-27 10:53:16,244 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 4.6.0 (camel-1) started in 29ms (build:0ms init:0ms start:29ms)
2024-08-27 10:53:16,282 INFO  [io.quarkus] (main) camel-kubernetes-vault 1.0-SNAPSHOT on JVM (powered by Quarkus 3.12.2) started in 3.337s. Listening on: http://0.0.0.0:8080
2024-08-27 10:53:16,282 INFO  [io.quarkus] (main) Profile prod activated. 
2024-08-27 10:53:16,283 INFO  [io.quarkus] (main) Installed features: [agroal, camel-attachments, camel-core, camel-google-secret-manager, camel-jackson, camel-kamelet, camel-kubernetes, camel-log, camel-microprofile-health, camel-platform-http, camel-rest, camel-rest-openapi, camel-sql, camel-yaml-dsl, cdi, kubernetes, kubernetes-client, narayana-jta, smallrye-context-propagation, smallrye-health, vertx]
2024-08-27 10:53:17,831 INFO  [route1] (Camel (camel-1) thread #1 - sql://select%20*%20from%20test;) {"data":"hello"}
2024-08-27 10:53:17,833 INFO  [route1] (Camel (camel-1) thread #1 - sql://select%20*%20from%20test;) {"data":"world"}
```

## Auto refresh of the secret and modification

To show how to refresh works we'll need to change the password for postgresadmin user on our Database.

First run the following command:

```
oc rsh $(oc get pods -l postgres-operator.crunchydata.com/role=master -o name)
```

Now you need to change the password inside the container

```
sh-4.4$ psql -U postgres -c "ALTER USER postgresadmin PASSWORD 'masteradmin12345*';"
```

At the same time modify the secret stored into AWS Secret Manager by editing the password field with 'masteradmin12345*' in the AWS console.

Now get back to the log and you should see the following entries:

```
2024-08-27 11:59:18,053 INFO  [route1] (Camel (camel-1) thread #1 - sql://select%20*%20from%20test;) {"data":"hello"}
2024-08-27 11:59:18,053 INFO  [route1] (Camel (camel-1) thread #1 - sql://select%20*%20from%20test;) {"data":"world"}
2024-08-27 12:01:18,061 INFO  [route1] (Camel (camel-1) thread #1 - sql://select%20*%20from%20test;) {"data":"hello"}
2024-08-27 12:01:18,062 INFO  [route1] (Camel (camel-1) thread #1 - sql://select%20*%20from%20test;) {"data":"world"}
```

If you look at the description of external secret you should see the secret has been updated:

```
oc describe es example
.
.
.
.
Status:
  Binding:
    Name:  authsecdb
  Conditions:
    Last Transition Time:   2024-08-27T09:44:24Z
    Message:                Secret was synced
    Reason:                 SecretSynced
    Status:                 True
    Type:                   Ready
  Refresh Time:             2024-08-27T12:02:44Z
  Synced Resource Version:  1-2d524f20b9a51515951be6fe2bc907a8
Events:
  Type    Reason   Age    From              Message
  ----    ------   ----   ----              -------
  Normal  Created  138m   external-secrets  Created Secret
  Normal  Updated  3m17s  external-secrets  Updated Secret

```
