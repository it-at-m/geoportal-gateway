# GDI Geoportal Gateway

The gateway is based on Spring Cloud and its primary purpose is to serve portals created by the gdi-admin-manager (a legacy software that is no longer being developed, while this component will operate independently of it).

A portal is a geo web application based on [Masterportal Hamburg](https://www.masterportal.org/). It shows an interactive map with multiple layers
and supports different other functionalities such as printing a part of the map or searching for an address.

The portals shown by the gateway are created with help of the gdi-admin-manager.
The admin-manager writes the portals into a file system, shared with the gateway, and saves information about the portals in a database.

The gateway serves these portals from the shared file system under <gateway-host>/portal/<portal-name>. 

### Authentication and authorization

The gateway checks for access permission.
If a portal is protected and the user is not logged in it will redirect the user to keycloak where a login mask is presented.
To get necessary permission requirements the gateway uses the database of the admin-manager.
Then it checks via the BVI if the user is logged in with the necessary authorization level and product role.

The portals use requests to access the geo layers and other functionalities (e.g. printing, searching, resource files).
These requests are filtered by the gateway and routed to other servers or answered directly.
During the filtering the gateway checks for the necessary permission and redirects to keycloak if needed.

For example request for geo layers will be routed to a geoserver. The gateway hereby filters the requests and checks if the user has the necessary rights.
In the following a more detailed example:

#### A detailed example: The Search

The map client has a search bar where one can perform an address or geo data search. If one enters some letters into the search bar, the map client will send a request containing the search string. 
The recipient of the request as well as additional parameters are defined in the config.json or the
rest-service.json of the portal written to the file system. In our case the gateway receives the request under the search endpoint specified in the [search controller](src/main/java/de/swm/lhm/geoportal/gateway/search/SearchController.java).
The gateway than sends one or multiple request to load-balanced elastic-search instances and returns the answer to the map client in the needed format.

Now how does authorization come into play? If you have a close look at the search controller you won't see anything in this regard.
The reason is that this happens by a security filter which is applied to search requests even before the controller gets the request.
For this Spring Cloud Security is used. The filter mechanism is specified in the [SearchSecurityConfig](src/main/java/de/swm/lhm/geoportal/gateway/search/SearchSecurityConfig.java) and consists of two parts:
1. A **matcher** implements the *ServerWebExchangeMatcher* from Spring Security, in this case the [SearchRequestMatcher](src/main/java/de/swm/lhm/geoportal/gateway/search/authorization/SearchRequestMatcher.java).
It checks if the request is a search request and if authorization is needed.
In the case of the search, the access rights of the search are the one of the portal.
The search request contains a portalId. With help of this id the admin-manager database is queried to access the needed authorization for the corresponding portal.
Usually database views are used for this purpose. In this case the *portal_product_roles_view* which contains the assigned products and general access information (see [AuthorizationInfoRepository](src/main/java/de/swm/lhm/geoportal/gateway/authorization/repository/AuthorizationInfoRepository.java))
If the portal corresponding to the search is public it will return a negative MatchResult, which means that no filter is applied.
If authorization is needed is will return a positive MatchResult including the needed authorization which means that the filter is applied.
2. The **filter** extending the AbstractAuthorizationManager implements the *ReactiveAuthorizationManager<AuthorizationContext>* from Spring Security. It will check if the user has the required product roles and the required authorization level. 
Here the filter is the [PortalRequestAuthorizationManager](src/main/java/de/swm/lhm/geoportal/gateway/portal/authorization/PortalRequestAuthorizationManager.java) 
In this case the PortalRequestAuthorizationManager can be used as the same the search should be able to be performed by users which can access the portal. If the user is authorized the AuthorizationManager returns a positive AuthorizationDecision. Thus, the request will go to the search controller. 
Otherwise, a negative AuthorizationDecision will be return a Forbidden error code.
If the user is not authenticated at all a redirect to keycloak will be initiated.

### Change from Netflix Zuul to Spring Cloud

The gateway is a reimplementation of gdi-geoportal-middleware-backend.
A reimplementation was necessary since the libraries from Netflix were no longer supported. Which made it also impossible to change to a newer Spring Boot and Java version.
This was a major security risk.

It also blocked the modernization of the admin-manager as the admin-manager and the middleware
used the shared library gdi-geoportal-config and hence needed to use the same Spring Boot version.
This dependency does no longer exists with the gateway. The Spring Boot version of the gateway and the admin-manager can be updated independently.

## Development

### Prepare application-dev.properties

- Rename [application-dev.yml.swm-sample](application-dev.yml.swm-sample)
  to [application-dev.yml](application-dev.yml)
- Change credentials of *admin-manager.datasource* to your local postgres instance
- Set config path of *style.icon.dir*
- Change password of *services.elastic-search* or replace with connection properties for your local elasticsearch
  instance

### Run locally

To run the gateway locally start the docker services:

```shell
cd docker
docker-compose up
```

and start the application:

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Testing and Coverage

#### Jacoco Coverage

Run

```
mvn test && mvn jacoco:report
```
to produce [jacoco test report](target/site/jacoco/index.html) which contains the line-coverage of all Java classes.

#### Pitest - Mutations Testing

This project uses [Pitest](https://pitest.org/) as mutation testing framework to validate the quality of the unit tests.

Run
```
mvn pitest:mutationCoverage && mvn pitest:report
```
to produce [pitest report](target/site/jacoco/index.html) which contains the pitest coverage report for all unit tested
code.

## Code Quality

This project uses spotless, pmd and checkstyle to ensure consistent formatting and coding styles. These checks are being performed during the **static code quality** workflow on each commit.


### PMD

[PMD](https://docs.pmd-code.org/) is a static source code analyzer. It finds common programming flaws like unused variables, empty catch blocks, unnecessary object creation, and so forth.

The ruleset is b

To check if there are PMD issues run

```shell
mvn pmd:check
```

### Error Prone

This project uses [Error Prone](https://errorprone.info/):
Error Prone is a static analysis tool for Java that catches common programming mistakes at compile-time.

Error prone is enabled by default in the maven build process and will produce WARNINGs and ERRORs in the build log.

When you encounter WARNINGs or ERRORs you can try to let ErrorProne fix the code automatically by changing

```
<arg>-Xplugin:ErrorProne</arg>
```
with
```
<arg>-Xplugin:ErrorProne -XepPatchLocation:IN_PLACE</arg>
```
in the pom.xml file and run

```shell
mvn clean install
```


### Checkstyle

[Checkstyle](https://checkstyle.org/index.html) can check many aspects of your source code. It can find class design problems, method design problems. It also has the ability to check code layout and formatting issues.

To check if there are PMD issues run

```shell
mvn checkstyle:check
```

### Spotless (Not yet active)
Spotless is a code formatter to ensure consistent code format across the project. This project uses the *Google Java Format*.

To check if the code is properly formatted run

```shell
mvn spotless:check
```

To reformat the entire project run
```shell
mvn spotless:apply
```

### Swagger UI

After starting the application locally the Swagger UI is available at

[http://localhost:8082/webjars/swagger-ui/index.html](http://localhost:8082/webjars/swagger-ui/index.html)

### Dependencies

To generate a report about the dependencies used in this project run
```
mvn project-info-reports:dependencies
```
which will produce a html-report in
[target/site/dependencies.html](target/site/dependencies.html)

To generate a report of all pending dependency updates run
```
mvn versions:display-dependency-updates
```