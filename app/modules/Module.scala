package modules

import com.google.inject.AbstractModule
import config.{AMLSAuthConnector, WSHttp}
import connectors.{DataCacheConnector, KeystoreConnector}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpPost

class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[HttpPost]).toInstance(WSHttp)
    bind(classOf[KeystoreConnector]).toInstance(KeystoreConnector)
    bind(classOf[AuthConnector]).to(classOf[config.FrontendAuthConnector])
  }
}
