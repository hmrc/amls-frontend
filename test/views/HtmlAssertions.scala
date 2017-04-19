package views

import org.jsoup.nodes.Element
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import scala.collection.JavaConversions._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

trait HtmlAssertions {
  self:MustMatchers =>

  def checkListContainsItems(parent:Element, keysToFind:Set[String]) = {
    val texts = parent.select("li").toSet.map((el:Element) => el.text())
    texts must be (keysToFind.map(k => Messages(k)))
    true
  }

  def checkElementTextIncludes(el:Element, keys : String*) = {
    val t = el.text()
    val l = el.getElementsByTag("a").attr("href")
    val p = l.substring(l.indexOf("?"))
    keys.foreach { k =>
      t must include (Messages(k))
      p must include("edit=true")
    }
    true
  }
}

