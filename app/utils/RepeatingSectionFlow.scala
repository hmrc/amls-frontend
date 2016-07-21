package utils

import play.api.mvc.PathBindable

sealed trait RepeatingSectionFlow {

  def isEdit = this == RepeatingSectionFlow.Edit
}

object RepeatingSectionFlow {

  case object Edit extends RepeatingSectionFlow

  case object Continue extends RepeatingSectionFlow

  case object Add extends RepeatingSectionFlow

  implicit val binder = new PathBindable[RepeatingSectionFlow] {
    override def bind(key: String, value: String): Either[String, RepeatingSectionFlow] = {
      value.toLowerCase() match {
        case "add" => Right(Add)
        case "continue" => Right(Continue)
        case "edit" => Right(Edit)
        case _ => Left("RepeatingSectionFlow Binding Failure")
      }
    }

    override def unbind(key: String, value: RepeatingSectionFlow): String = {
      value match {
        case Add      => "add"
        case Continue => "continue"
        case Edit     => "edit"
      }
    }
  }
}

