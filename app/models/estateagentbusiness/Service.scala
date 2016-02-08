package models.estateagentbusiness

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
case object Residential extends Service

object Service {

 implicit def fromString(str : String, form:UrlFormEncoded) : Option[Service] = {
    str match {
      case "01" => Some(Residential)
      case "02" => Some(Commercial)
      case "03" => Some(Auction)
      case "04" => Some(Relocation)
      case "05" => Some(BusinessTransfer)
      case "06" => Some(AssetManagement)
      case "07" => Some(LandManagement)
      case "08" => Some(Development)
      case "09" => Some(SocialHousing)
      case _ => None
    }
  }

  implicit def fromString(str : String) : Service = {
    str match {
      case "01" => Residential
      case "02" => Commercial
      case "03" => Auction
      case "04" => Relocation
      case "05" => BusinessTransfer
      case "06" => AssetManagement
      case "07" => LandManagement
      case "08" => Development
      case "09" => SocialHousing
    }
  }

  implicit def servicesToString(obj : Service) : String = {
   obj match {
     case Residential => "01"
     case Commercial => "02"
     case Auction => "03"
     case Relocation => "04"
     case BusinessTransfer => "05"
     case AssetManagement => "06"
     case LandManagement => "07"
     case Development => "08"
     case SocialHousing => "09"
     case _ => ""
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


  implicit val jsonReads: Reads[Seq[Service]] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "services").read[Seq[String]].flatMap {
      x => Reads(i => JsSuccess(x.map(fromString)))
    }
  }

  implicit val jsonWrites = Writes[Seq[Service]] {
    case services => Json.obj("services" -> services.map(servicesToString))
  }
}