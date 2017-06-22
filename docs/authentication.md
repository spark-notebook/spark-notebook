User Login (Authentication):
----

Spark-notebook supports quite a few ways to login via the [`pac4j`](https://github.com/pac4j/pac4j) and [`play-pac4j`](https://github.com/pac4j/play-pac4j) libs (but it does not store a user database by itself).

Currently, the methods supported out of the box  are: 
- `HTTP Basic`
- `Form` (user & password login)
- `Kerberos/SPNEGO Negotiate`

and many more supported by `pac4j` (see below).
 
### Selecting one of supported auth modules

In `conf/application.conf`, add this:

```
# Security with pac4j config below
# ~~~~~
play.modules.enabled += "modules.SecurityModule"
play.http.filters = "filters.Filters"

# choose one of pac4j auth modules - this will define how user will log-in
notebook.server.auth.main_module = "FormClient"
//notebook.server.auth.main_module = "IndirectBasicAuthClient"
//notebook.server.auth.main_module = "IndirectKerberosClient"

# these define the URLs protected by security filters
pac4j.security {
  rules = [
    # The login page(s) needs to be publicly accessible.
    {"/loginForm.*" = {
      authorizers = "_anonymous_"
    }}
    {"/callback.*" = {
      authorizers = "_anonymous_"
    }}

    # A 'Catch all' rule to make sure the whole application stays secure.
    {".*" = {
      authorizers = "_authenticated_"
      clients = ${notebook.server.auth.main_module}
    }}
  ]
}
```

#### Auth with Simple Single User password

If `FormClient/IndirectBasicAuthClient` is enabled, configure the single user login:
```
# To be used with IndirectBasicAuthClient or with FormClient
notebook.server.auth.SingleUserPassAuthenticator {
  username = "admin"
  password = "pass"
}
```
or, if this is not enough, you may define your own more complex authenticator (as mentioned above).
See [`SecurityModule.scala`](../app/modules/SecurityModule.scala) file.

#### Auth with Kerberos
If enabled, you must set the keytab file and a principal in `conf/application.conf`:

```
// Configure the Kerberos login, if enabled
notebook.server.auth.KerberosAuthentication {
  service_principal = "HTTP/somehost.realm.com@REALM.COM"
  service_keytab = "/path-to/some-protected-file.keytab"
}
```

Make sure the exact service pricipal is contained in the keytab, and the keytab file is readable (but protected).

Beware the errors provided by Kerberos are most of the time not intuitive at all.
 See [`pac4j-kerberos` docs](http://www.pac4j.org/docs/clients/kerberos.html) for more details and troubleshooting.


#### Other authentication methods

With very minor additions to [`SecurityModule.scala`](../app/modules/SecurityModule.scala) file
 one could use any of the other methods supported by [`pac4j` v2.x](https://github.com/pac4j/pac4j):
- OAuth (Facebook, Twitter, Google...)
- SAML
- CAS
- OpenID Connect  & OpenID
- Google App Engine
- IP address
- and even LDAP, SQL, MongoDB, ... (as authenticators)
