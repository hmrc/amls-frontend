package models.estateagentbusiness


import play.api.data.mapping.forms._
import play.api.data.mapping._
import play.api.data.validation.ValidationError

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

  implicit def fromString(str : String) : Option[Service] = {
    str match {
      case "01" => Some(Commercial)
      case "02" => Some(Auction)
      case "03" => Some(Relocation)
      case "04" => Some(Auction)
      case "05" => Some(AssetManagement)
      case "06" => Some(LandManagement)
      case "07" => Some(Development)
      case "08" => Some(SocialHousing)
      case _ => None
    }
  }

  implicit val servicesFormRule : Rule[UrlFormEncoded, Seq[Service]] = new Rule[UrlFormEncoded, Seq[Service]] {
    def validate(form : UrlFormEncoded) : Validation[(Path, Seq[ValidationError]), Seq[Service]] = {

      form.getOrElse("services", Nil)
          .foldLeft[(Seq[ValidationError], Seq[Service])](Nil, Nil)((results, next) => {
                    fromString(next)
                      .map(service => (results._1, results._2 :+ service))
                      .getOrElse((results._1 :+ ValidationError(s"Invalid Service Type String $next"), results _2))
          }) match {
        case (Nil, services) => Success(services)
        case (err, _) => Failure(Seq(Path \ "services" -> err))


      }
    }
  }
}


