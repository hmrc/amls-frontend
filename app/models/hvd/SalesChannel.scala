package models.hvd

sealed trait SalesChannel

case object Retail extends SalesChannel

case object Wholesale extends SalesChannel

case object Auction extends SalesChannel

