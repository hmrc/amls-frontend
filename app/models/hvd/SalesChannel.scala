package models.hvd

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait SalesChannel

case object Retail extends SalesChannel

case object Wholesale extends SalesChannel

case object Auction extends SalesChannel

