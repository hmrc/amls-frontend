package models.payments

import play.api.mvc.{Call, Request}

class ReturnLocation(val url: String, val returnUrl: String)

object ReturnLocation {

  def apply(url: String)(implicit request: Request[_]) =
    new ReturnLocation(url, buildRedirectUrl(url))

  def apply(call: Call)(implicit request: Request[_]) =
    new ReturnLocation(call.url, buildRedirectUrl(call.url))

  private def buildRedirectUrl(url: String)(implicit request: Request[_]): String = {
    "^localhost".r.findFirstIn(request.host) match {
      case Some(_) => s"//${request.host}$url"
      case _ => url
    }
  }
}
