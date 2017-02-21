package utils

object HtmlElementHelpers {

  val digitsOnlyAttributes = Map("digits" -> "true")

  implicit class MapHelpers(map: Map[String, _]) {
    def toDataAttributes = map.map(t => s"data-${t._1}=${t._2}")
  }

}
