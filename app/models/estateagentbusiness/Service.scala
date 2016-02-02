package models.estateagentbusiness


import play.api.data.mapping
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait Service

case object Commercial extends Service
case object Auction extends Service
case object Relocation extends Service
case object BusinessTransfer extends Service
case object AssetManagement extends Service
case object LandManagement extends Service
case object Development extends Service
case object SocialHousing extends Service
case class Residential(redressScheme: Option[RedressScheme]) extends Service

object Service {

  implicit def fromString(str : String, form:UrlFormEncoded) : Option[Service] = {
    str match {
      case "01" => Some(Commercial)
      case "02" => Some(Auction)
      case "03" => Some(Relocation)
      case "04" => Some(Auction)
      case "05" => Some(AssetManagement)
      case "06" => Some(LandManagement)
      case "07" => Some(Development)
      case "08" => Some(SocialHousing)
      case "09" => Some(Residential(None))
      case _ => None
    }
  }

  implicit def servicesToString(obj : Service) : String = {
    obj match {
      case Commercial => "01"
      case Auction => "02"
      case Relocation => "03"
      case Auction => "04"
      case AssetManagement => "05"
      case LandManagement => "06"
      case Development => "07"
      case SocialHousing => "08"
      case Residential(None) => "09"
    }
  }

  implicit val servicesFormRule : Rule[UrlFormEncoded, Seq[Service]] = new Rule[UrlFormEncoded, Seq[Service]] {
    def validate(form : UrlFormEncoded) : Validation[(Path, Seq[ValidationError]), Seq[Service]] = {

      form.getOrElse("services", Nil)
          .foldLeft[(Seq[ValidationError], Seq[Service])](Nil, Nil)((results, next) => {
                    fromString(next,form)
                      .map(service => (results._1, results._2 :+ service))
                      .getOrElse((results._1 :+ ValidationError(s"Invalid Service Type String $next"), results _2))
          }) match {
        case (Nil, services) => Success(services)
        case (err, _) => Failure(Seq(Path \ "services" -> err))
      }
    }
  }

  implicit val formWrites: Write[Seq[Service], UrlFormEncoded]= Write {
    case services => Map("services" -> services.map(servicesToString))
  }

  val jsonServiceReads = Rule.fromMapping[JsValue, Service] {
    case JsString(x) => fromString(x, Map("gfklhj" -> Seq("fghg")))
                          .map(x => Success(x))
                          .getOrElse(Failure(Seq(ValidationError("error.required"))))

    case _ => Failure(Seq(ValidationError("Non JsString value passed into conversion")))
  }

  implicit val jsonReads = From[JsValue] { __ =>
    import play.api.libs.json.Reads.StringReads
    import play.api.data.mapping.json.Rules._
     (__ \ "services").read(seqR(jsonServiceReads))
  }

  implicit val jsonWrites = Writes[Seq[Service]] {
    case services => Json.obj("services" -> services.map(servicesToString))
    case _ => JsNull
  }
}


