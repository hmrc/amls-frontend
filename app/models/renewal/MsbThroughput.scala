package models.renewal

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.forms.Writes._
import play.api.libs.json.Json
import utils.MappingUtils.Implicits._

case class MsbThroughput(throughputSelection: String)

object MsbThroughput {

  implicit val format = Json.format[MsbThroughput]

  implicit val formReader: Rule[UrlFormEncoded, MsbThroughput] = From[UrlFormEncoded] { __ =>
    (__ \ "throughputSelection").read[String] map MsbThroughput.apply
  }

  implicit val formWriter: Write[MsbThroughput, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    (__ \ "throughputSelection").write[String] contramap(_.throughputSelection)
  }

}


