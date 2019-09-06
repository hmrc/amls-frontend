package config

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.mvc.Call
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

class WhitelistFilter @Inject()(val appConfig: AppConfig, implicit val mat: Materializer) extends AkamaiWhitelistFilter {
  override def whitelist: Seq[String] = appConfig.whitelist
  override def destination: Call = Call("GET", "https://www.gov.uk")
}
