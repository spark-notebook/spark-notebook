package modules.authentication

import org.pac4j.core.context.{Pac4jConstants, WebContext}
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.exception.CredentialsException
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.util.CommonHelper

// based on SimpleTestUsernamePasswordAuthenticator from pac4j
class SingleUserPassAuthenticator(authConfig: play.api.Configuration) extends Authenticator[UsernamePasswordCredentials] {
  private val expectUser = authConfig.getString("username").get
  private val expectPass = authConfig.getString("password").get

  protected def throwException(message: String): Unit = {
    throw new CredentialsException(message)
  }

  def validate(credentials: UsernamePasswordCredentials, context: WebContext): Unit = {
    if (credentials == null) throwException("No credential")
    val username = credentials.getUsername
    val password = credentials.getPassword
    if (CommonHelper.isBlank(username)) throwException("Username cannot be blank")
    if (CommonHelper.isBlank(password)) throwException("Password cannot be blank")
    if (!(expectUser == username && expectPass == password)) throwException("Invalid Username or password")

    // store the logged in user in the session
    val profile = new CommonProfile
    profile.setId(username)
    profile.addAttribute(Pac4jConstants.USERNAME, username)
    credentials.setUserProfile(profile)
  }
}
