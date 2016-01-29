package models.estateagentbusiness


import com.fasterxml.jackson.databind.deser.std.StringArrayDeserializer
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
  implicit val serviceFormRule : Rule[UrlFormEncoded, Seq[Service]] = From[UrlFormEncoded, Seq[Service]] (
    reader =>
  )


   /*implicit val serviceFormRule : Rule[UrlFormEncoded, Service] = From[UrlFormEncoded]   { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "sdsdfkj").read[Seq[String]] fmap { a => a map {
                                            case "01" => Commercial
                                            case "02" => Auction
                                            case "03" => Relocation
                                            case "04" => Auction
                                            case "05" => AssetManagement
                                            case "06" => LandManagement
                                            case "07" => Development
                                            case "08" => SocialHousing

                                          }}

  }*/
  import utils.MappingUtils.Implicits._
  //implicit val formRule: Rule[UrlFormEncoded, Seq[Service]] = (Path \ "Services").read[UrlFormEncoded, Seq[Service]]

  //implicit val formRuleStr: Rule[UrlFormEncoded, String] =  (Path \ "services").read[UrlFormEncoded, String]


  //(Path \ "services").read[UrlFormEncoded, Seq[String]]


//  { __ =>
//
//
////    import play.api.data.mapping.forms.Rules._
////    (__ \ "services1").read[String] fmap { case "01" =>print("Yesssssss11111")
////      Commercial }
////    (__ \ "services2").read[String] fmap { case "02" => print("Yesssssss22222222")
////      Auction }
////    (__ \ "services3").read[String] fmap { case "03" => print("Yesssssss333333333333")
////      Relocation  case _ => SocialHousing
//    }
//   }

}


