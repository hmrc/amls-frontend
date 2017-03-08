package models.payments

import play.api.mvc.Cookie

case class PaymentServiceRedirect(url: String, responseCookies: Seq[Cookie] = Seq.empty)
