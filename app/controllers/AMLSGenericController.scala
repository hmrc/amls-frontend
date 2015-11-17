package controllers

import controllers.auth.AmlsRegime
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait AMLSGenericController extends FrontendController with Actions {

  protected def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result]

  protected def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result]

  def get(): Action[AnyContent] =
    AuthorisedFor(AmlsRegime).async {
      user => request => get(user, request)
    }

  def post(): Action[AnyContent] =
    AuthorisedFor(AmlsRegime).async {
      user => request => post(user, request)
    }

  implicit val booleanFormatter: Formatter[Boolean] = new Formatter[Boolean] {

    override val format = Some(("format.boolean", Nil))

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
      data.get(key) match {
        case Some(value) => value match {
          case "true" => Right(true)
          case "false" => Right(false)
          case _ => Left(Seq(FormError(key, "error.boolean", Nil)))
        }
        case _ => Left(Seq(FormError(key, "error.boolean.empty", Nil)))
      }

    override def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
  }
}
