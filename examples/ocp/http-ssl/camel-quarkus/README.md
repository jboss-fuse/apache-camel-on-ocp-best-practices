## Securing Camel routes with TLS certificates using the cert-manager Operator for Red Hat OpenShift

In this sample you'll use the cert-manager Operator for Red Hat OpenShift to secure communication between two peers
(client and server) in OpenShift. 

### Prerequisites

- oc client [installed](https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/getting-started-cli.htmlguide) to execute remote operations.
- already logged in into cluster (successfully running _oc login_)
- cert-manager Operator for Red Hat OpenShift [installed](https://docs.openshift.com/container-platform/latest/security/cert_manager_operator/cert-manager-operator-install.html) and configured correctly.

### Preparing the project

We'll connect to the `camel-http-ssl-qk` project and check the installation status. To change project, open a terminal tab and type the following command:

```
oc new-project camel-http-ssl-qk
```

### Create certificates

To set up the certificates and keystores, the cert-manager Operator need to be installed, please refer to the official guide linked above.

Once the operator is installed, you can create the issuer running
```
oc apply -f ../resources/issuer.yaml
```
then verify the issuer is ready (optional)
```
oc get issuers -n camel-http-ssl-qk
```
create secret containing password for the keys
```
oc create secret generic http-ssl-example-tls-password --from-literal=password=pass123
```

generate server certificate

```
oc apply -f ../resources/certificate-server.yaml
```

verify server certificate in the managed secret (optional)

```
oc get secret ocp-ssl-camel-server-tls
```

generate client certificate

```
oc apply -f ../resources/certificate-client.yaml
```

verify client certificate in the managed secret (optional)

```
oc get secret ocp-ssl-client-tls
```

create a role and role binding to access to the secrets, bound to the service account named `secret-reader` used to run applications

```
oc apply -f role.yaml
```

## Deploy to OCP

Deploy the server on OCP:

```
./mvnw install -Dquarkus.openshift.deploy=true -f server/pom.xml
```

Deploy client on OCP:

```
./mvnw install -Dquarkus.openshift.deploy=true -f client/pom.xml
```

to test the application, call the API exposed by the client, to start the handshake between client and server:

```
curl "http://$(oc get route camel-http-ssl-client -o go-template --template='{{.spec.host}}')/ping"
pong
```