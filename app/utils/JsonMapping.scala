package utils

import play.api.data.mapping._
import play.api.data.validation.ValidationError

trait JsonMapping extends GenericRules {

  import play.api.libs.json
  import play.api.libs.json.{JsPath, JsValue, Reads, Writes, JsSuccess, JsError}
  import play.api.data.mapping.{KeyPathNode, IdxPathNode, PathNode}

  implicit def nodeToJsNode(n: PathNode): json.PathNode = {
    n match {
      case KeyPathNode(key) =>
        json.KeyPathNode(key)
      case IdxPathNode(idx) =>
        json.IdxPathNode(idx)
    }
  }

  private def pathToJsPath(p: Path): JsPath =
    JsPath(p.path.map(nodeToJsNode _))

  implicit def errorConversion(errs: Seq[(Path, Seq[ValidationError])]): Seq[(JsPath, Seq[ValidationError])] =
    errs map {
      case (path, errors) =>
        (pathToJsPath(path), errors)
    }

  implicit def jsonR[A]
  (implicit
   rule: Rule[JsValue, A]
  ): Reads[A] =
    Reads {
      json =>
        rule.validate(json) match {
          case Success(a) =>
            JsSuccess(a)
          case Failure(errors) =>
            JsError(errors)
        }
    }

  implicit def jsonW[A]
  (implicit
   write: Write[A, JsValue]
  ): Writes[A] =
    Writes {
      a =>
        write.writes(a)
    }
}

object JsonMapping extends JsonMapping
