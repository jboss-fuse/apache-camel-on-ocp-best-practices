## Azure Key Vault Secret Retrieval and refresh on OCP

In this sample you'll use the Azure Key Vault Properties Source and export the simple route to OCP

## Preparing the project

We'll connect to the `camel-sql` project and check the installation status. To change project, open a terminal tab and type the following command:

```
oc new-project camel-sql
```

## Setting up Database

This example uses a PostgreSQL database. We want to install it on the project `camel-sql`. We can go to the OpenShift 4.x WebConsole page, use the OperatorHub menu item on the left hand side menu and use it to find and install "Crunchy Postgres for Kubernetes". This will install the operator and may take a couple of minutes to install.

Once the operator is installed, we can create a new database using

```
oc create -f postgres.yaml
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

Now, we need to find out Postgres username, password and hostname and prepare the Azure Key Vault Secret and credentials.

```
USER_NAME=$(oc get secret postgres-pguser-postgresadmin --template={{.data.user}} | base64 -d)
USER_PASSWORD=$(oc get secret postgres-pguser-postgresadmin --template={{.data.password}} | base64 -d)
HOST=$(oc get secret postgres-pguser-postgresadmin --template={{.data.host}} | base64 -d)
PASSWORD_SKIP_SPEC_CHAR=$(sed -e 's/[&\\/]/\\&/g; s/$/\\/' -e '$s/\\$//' <<<"$USER_PASSWORD")
```

Now we need to create the secret payload. Open the file postgresCreds.json and place the related exported value into the related field. You should have something like:

```
{
  "username": "user",
  "password": "password",
  "host": "host"
}
```

First of all we need to create an application

```
az ad app create --display-name test-app-key-vault
```

Then we need to obtain credentials

```
az ad app credential reset --id <appId> --append --display-name 'Description: Key Vault app client' --end-date '2024-12-31'
```

This will return a result like this


```
{
  "appId": "appId",
  "password": "pwd",
  "tenant": "tenantId"
}
```

You should take note of the password and use it as clientSecret parameter in the azure-keyvault-secrets.yaml file, together with the clientId and tenantId.

Now create the key vault

```
az keyvault create --name <vaultName> --resource-group <resourceGroup> --enable-rbac-authorization false
```

Create a service principal associated with the application Id

```
az ad sp create --id <appId>
```

At this point we need to add a role to the application with role assignment

```
az role assignment create --assignee <appId> --role "Key Vault Administrator" --scope /subscriptions/<subscriptionId>/resourceGroups/<resourceGroup>/providers/Microsoft.KeyVault/vaults/<vaultName>
```

Last step is to create policy on what can be or cannot be done with the application. In this case we just want to read the secret value. So This should be enough.

```
az keyvault set-policy --name <vaultName> --spn <appId> --secret-permissions get
```

You can create a secret through Azure CLI with the following command:

```
az keyvault secret set --name authsecdbref --vault-name <vaultName> -f postgresCreds.json
```

Where the content of secret.json could be something like:

```
{
  "username": "postgresadmin",
  "password": "xxxx",
  "host": "host"
}
```

Now create the secret.

```
oc apply -f azure-keyvault-secrets.yaml
```

Don't forget to modify the keyVault name in the application.properties file.

Now we need to setup the Eventhub/EventGrid notification for being informed about secrets updates.

First of all we'll need a Blob account and Blob container, to track Eventhub consuming activities.

```
az storage account create --name <blobAccountName> --resource-group <resourceGroup>
```

Then create a container

```
az storage container create --account-name <blobAccountName> --name <blobContainerName>
```

Then recover the access key for this purpose

```
az storage account keys list -g <resourceGroup> -n <blobAccountName>
```

Substitute the blob Account name, blob Container name and Blob Access Key into the application.properties file.

Let's now create the Eventhub side

Create the namespace first

```
az eventhubs namespace create --resource-group <resourceGroup> --name <eventhub-namespace> --location westus --sku Standard --enable-auto-inflate --maximum-throughput-units 20
```

Now create the resource

```
az eventhubs eventhub create --resource-group <resourceGroup> --namespace-name <eventhub-namespace> --name <eventhub-name> --cleanup-policy Delete --partition-count 15
```

Now we need to create a shared policy to access Event Hub.

```
az eventhubs eventhub authorization-rule create --resource-group <resourceGroup> --namespace-name <eventhub-namespace> --eventhub-name <eventhub-name>  --name <auth_rule_name> --rights Listen Send Manage
```

Now we are ready to get the connection string

```
az eventhubs eventhub authorization-rule keys list --resource-group <resourceGroup> --namespace-name <eventhub-namespace> --eventhub-name <eventhub-name> --name <auth_rule_name>
```

This will return the following output

```
{
  "keyName": "<auth_rule_name>",
  "primaryConnectionString": "<primary_conn_string>",
  "primaryKey": "<primary_key>",
  "secondaryConnectionString": "<second_conn_string>",
  "secondaryKey": "<secondary_key>"
}

```

Substitute the connection string field with <primary_conn_string> into the application.properties.

Get back to the Azure Portal, and go to Key Vault service.

Select the Key Vault just created. In the menu select "Events".

Then Select the Event Hub icon.

In the page that will open, define a name for the event subscription for example "keyvault-to-eh".

In the System topic name field add "keyvault-to-eh-topic" for example.

In the "Filter to Event Types" leave the default value of 9.

In the configure endpoint section for Eventhub, in the Event Hub namespace section you should notice the namespace you've created through the AZ CLI, select that and in the Event Hub dropdown menu select the Event Hub you've created through the AZ CLI. Press confirm selection. 

Leave everything as it is and press "Create".

This complete the Database and secret refresh setup.

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
2024-08-09 05:12:07,214 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) Bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2024-08-09 05:12:07,217 INFO  [org.apa.cam.mai.MainSupport] (main) Apache Camel (Main) 4.6.0 is starting
2024-08-09 05:12:07,355 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Auto-configuration summary
2024-08-09 05:12:07,356 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [MicroProfilePropertiesSource] camel.main.routesIncludePattern=camel/sql-to-log.camel.yaml
2024-08-09 05:12:07,356 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [MicroProfilePropertiesSource] camel.vault.azure.vaultName=az-key-vault-456789
2024-08-09 05:12:07,356 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [MicroProfilePropertiesSource] camel.vault.azure.azureIdentityEnabled=true
2024-08-09 05:12:07,572 INFO  [com.azu.ide.EnvironmentCredential] (main) Azure Identity => EnvironmentCredential invoking ClientSecretCredential
2024-08-09 05:12:08,983 WARN  [com.mic.aad.msa.ConfidentialClientApplication] (ForkJoinPool.commonPool-worker-1) [Correlation ID: b2435de4-22a2-42eb-90d5-4f4c3764ab79] Execution of class com.microsoft.aad.msal4j.AcquireTokenSilentSupplier failed: Token not found in the cache
2024-08-09 05:12:09,162 INFO  [com.azu.ide.ChainedTokenCredential] (main) Azure Identity => Attempted credential EnvironmentCredential returns a token
2024-08-09 05:12:09,163 INFO  [com.azu.cor.imp.AccessTokenCache] (main) {"az.sdk.message":"Acquired a new access token."}
2024-08-09 05:12:09,901 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 4.6.0 (camel-1) is starting
2024-08-09 05:12:09,922 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Property-placeholders summary
2024-08-09 05:12:09,923 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] query=select * from test;
2024-08-09 05:12:09,923 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] dsBean=dsBean-1
2024-08-09 05:12:09,923 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] delay=5000
2024-08-09 05:12:09,923 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] password=xxxxxx
2024-08-09 05:12:09,924 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] serverName=postgres-primary.camel-sql.svc
2024-08-09 05:12:09,924 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] databaseName=test
2024-08-09 05:12:09,924 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     [stgresql-source.kamelet.yaml] username=xxxxxx
2024-08-09 05:12:09,925 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Routes startup (total:1 started:1 kamelets:1)
2024-08-09 05:12:09,926 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started route1 (kamelet://postgresql-source)
2024-08-09 05:12:09,926 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 4.6.0 (camel-1) started in 24ms (build:0ms init:0ms start:24ms)
2024-08-09 05:12:09,969 INFO  [io.quarkus] (main) camel-azure-keyvault-vault 1.0-SNAPSHOT on JVM (powered by Quarkus 3.12.2) started in 5.176s. Listening on: http://0.0.0.0:8080
2024-08-09 05:12:09,969 INFO  [io.quarkus] (main) Profile prod activated. 
2024-08-09 05:12:09,970 INFO  [io.quarkus] (main) Installed features: [agroal, camel-attachments, camel-azure-key-vault, camel-core, camel-jackson, camel-kamelet, camel-kubernetes, camel-log, camel-microprofile-health, camel-platform-http, camel-rest, camel-rest-openapi, camel-sql, camel-yaml-dsl, cdi, kubernetes, kubernetes-client, narayana-jta, smallrye-context-propagation, smallrye-health, vertx]
2024-08-09 05:12:11,413 INFO  [route1] (Camel (camel-1) thread #1 - sql://select%20*%20from%20test;) {"data":"hello"}
2024-08-09 05:12:11,416 INFO  [route1] (Camel (camel-1) thread #1 - sql://select%20*%20from%20test;) {"data":"world"}

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

At the same time modify the secret stored into the file postgresCreds.json with the new password and run:

```
az keyvault secret set --name authsecdbref --vault-name <keyvaultName> -f postgresCreds.json
```

Now get back to the log and you should see the following entries:

```
2024-08-09 05:45:52,758 INFO  [org.apa.cam.com.azu.key.vau.EventhubsReloadTriggerTask] (partition-pump-1-21) Update for Azure secret: authsecdbref detected, triggering CamelContext reload
2024-08-09 05:45:52,759 INFO  [org.apa.cam.sup.DefaultContextReloadStrategy] (partition-pump-1-21) Reloading CamelContext (camel-1) triggered by: Azure Secrets Refresh Task
2024-08-09 05:45:54,643 INFO  [route1] (Camel (camel-1) thread #5 - sql://select%20*%20from%20test;) {"data":"hello"}
2024-08-09 05:45:54,643 INFO  [route1] (Camel (camel-1) thread #5 - sql://select%20*%20from%20test;) {"data":"world"}
```


