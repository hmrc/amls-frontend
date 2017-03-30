package models.renewal

import play.api.libs.json.Json

case class MsbThroughput(throughputSelection: String)

object MsbThroughput {

  implicit val format = Json.format[MsbThroughput]

}


