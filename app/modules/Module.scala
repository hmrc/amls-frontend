package modules

import com.google.inject.AbstractModule
import config.WSHttp
import connectors.KeystoreConnector
import uk.gov.hmrc.play.http.HttpPost

class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[HttpPost]).toInstance(WSHttp)
    bind(classOf[KeystoreConnector]).toInstance(KeystoreConnector)
  }
}
