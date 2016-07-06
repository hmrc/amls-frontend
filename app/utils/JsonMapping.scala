package utils

import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

trait JsonMapping {

  import play.api.libs.json
  import play.api.libs.json.{JsPath, JsValue, Reads, Writes, JsSuccess, JsError}
  import play.api.data.mapping.{KeyPathNode, IdxPathNode, PathNode}

  def nodeToJsNode(n: PathNode): json.PathNode = {
    n match {
      case KeyPathNode(key) =>
        json.KeyPathNode(key)
      case IdxPathNode(idx) =>
        json.IdxPathNode(idx)
    }
  }

  def jsNodeToNode(n: json.PathNode): PathNode = {
    n match {
      case json.KeyPathNode(key) =>
        KeyPathNode(key)
      case json.IdxPathNode(idx) =>
        IdxPathNode(idx)
    }
  }

  private def pathToJsPath(p: Path): JsPath =
    JsPath(p.path.map(nodeToJsNode _))

  private def jsPathToPath(p: JsPath): Path =
    Path(p.path.map(jsNodeToNode _))

  implicit def errorConversion(errs: Seq[(Path, Seq[ValidationError])]): Seq[(JsPath, Seq[ValidationError])] =
    errs map {
      case (path, errors) =>
        (pathToJsPath(path), errors)
    }

  implicit def errorConversion2(errs: Seq[(JsPath, Seq[ValidationError])]): Seq[(Path, Seq[ValidationError])] =
    errs map {
      case (path, errors) =>
        (jsPathToPath(path), errors)
    }

  implicit def genericJsonR[A]
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

  implicit def genericJsonW[A]
  (implicit
   write: Write[A, JsValue]
  ): Writes[A] =
    Writes {
      a =>
        write.writes(a)
    }

  // This is here to prevent NoSuchMethodErrors from the validation library
  implicit def pickInJson[II <: JsValue, O](p: Path)(implicit r: RuleLike[JsValue, O]): Rule[II, O] = {

    def search(path: Path, json: JsValue): Option[JsValue] = path.path match {
      case KeyPathNode(k) :: t =>
        json match {
          case JsObject(js) =>
            js.find(_._1 == k).flatMap(kv => search(Path(t), kv._2))
          case _ => None
        }
      case IdxPathNode(i) :: t =>
        json match {
          case JsArray(js) => js.lift(i).flatMap(j => search(Path(t), j))
          case _ => None
        }
      case Nil => Some(json)
    }

    Rule[II, JsValue] { json =>
      search(p, json) match {
        case None => Failure(Seq(Path -> Seq(ValidationError("error.required"))))
        case Some(js) => Success(js)
      }
    }.compose(r)
  }

//  implicit def readsToRule[A]
//  (implicit
//   r: Reads[A]
//  ): Rule[JsValue, A] =
//    Rule {
//      i =>
//        r.reads(i) match {
//          case JsSuccess(v, _) => Success(v)
//          case JsError(errors) => Failure(errors)
//        }
//    }
}

object JsonMapping extends JsonMapping
