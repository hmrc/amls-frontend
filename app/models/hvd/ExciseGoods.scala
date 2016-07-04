package models.hvd

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class ExciseGoods(exciseGoods: Boolean)

object ExciseGoods {

  implicit val format = Json.format[ExciseGoods]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExciseGoods] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "exciseGoods").read[Boolean].withMessage("error.required.hvd.excise.goods") fmap ExciseGoods.apply
  }

  implicit val formWrites: Write[ExciseGoods, UrlFormEncoded] = Write {
    case ExciseGoods(registered) => Map("exciseGoods" -> Seq(registered.toString))
  }

}
