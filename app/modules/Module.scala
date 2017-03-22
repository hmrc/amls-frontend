package modules

import com.google.inject.AbstractModule
import config.WSHttp
import connectors.{AuthConnector, DataCacheConnector, KeystoreConnector}
import services.ProgressService
import uk.gov.hmrc.play.http.HttpPost

class Module extends AbstractModule {

  type HmrcAuthConnector = uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

  override def configure() = {
    bind(classOf[HttpPost]).toInstance(WSHttp)
    bind(classOf[KeystoreConnector]).toInstance(KeystoreConnector)
    bind(classOf[DataCacheConnector]).toInstance(DataCacheConnector)
    bind(classOf[HmrcAuthConnector]).to(classOf[config.FrontendAuthConnector])
    bind(classOf[connectors.AuthConnector]).toInstance(AuthConnector)
    bind(classOf[ProgressService]).toInstance(ProgressService)
  }
}
