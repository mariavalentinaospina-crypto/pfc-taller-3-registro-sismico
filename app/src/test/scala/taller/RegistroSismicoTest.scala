package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import Datos.registro

@RunWith(classOf[JUnitRunner])
class RegistroSismicoTest extends AnyFunSuite {

  val s = new RegistroSismico()
  import s._

  val vacio: List[Sismo] = List()

  // Punto 1 -------------------------------------------------------------------

  test("fuertes: los que superan 5.0, en orden") {
    assert(fuertes(registro, 5.0) == List("Santander", "Choco", "Valle"))
  }

  test("fuertes: con un umbral alto no queda ninguno") {
    assert(fuertes(registro, 9.0) == List())
  }

  test("fuertes: conserva los repetidos") {
    assert(fuertes(registro, 3.0).length == 7)
  }

  test("fuertes: el umbral es estricto") {
    // Hay un sismo de magnitud 6.0 exacta; con umbral 6.0 no debe salir.
    assert(!fuertes(registro, 6.0).contains("Choco"))
  }

  test("enRegion: las magnitudes del Valle") {
    assert(enRegion(registro, "Valle") == List(4.2, 3.8, 5.3))
  }

  test("enRegion: una región que no está da la lista vacía") {
    assert(enRegion(registro, "Amazonas") == List())
  }

  test("enRegion: sobre el registro vacío no hay magnitudes") {
    assert(enRegion(vacio, "Valle") == List())
  }

  test("paresCercanos: con 0.6 solo Valle y Santander") {
    assert(paresCercanos(registro, 0.6) == List(("Valle", "Santander")))
  }

  test("paresCercanos: con 1.0 salen las tres parejas, en orden de aparición") {
    assert(paresCercanos(registro, 1.0) ==
      List(("Valle", "Santander"), ("Valle", "Choco"), ("Santander", "Choco")))
  }

  test("paresCercanos: con 0.1 no hay ninguna") {
    assert(paresCercanos(registro, 0.1) == List())
  }

  test("paresCercanos: cada pareja sale una sola vez") {
    val ps = paresCercanos(registro, 1.0)
    assert(ps.map { case (a, b) => Set(a, b) }.distinct.length == ps.length)
  }

  test("paresCercanos: nunca empareja una región consigo misma") {
    assert(paresCercanos(registro, 5.0).forall { case (a, b) => a != b })
  }

  // Punto 1 Propio ------------------------------------------------------------

  test("P1-1 fuertes: umbral negativo deja pasar todos los sismos") {
    assert(fuertes(registro, -1.0).length == 7)
  }

  test("P1-2 fuertes: umbral exactamente igual a una magnitud es estricto") {
    assert(!fuertes(registro, 5.3).contains("Valle"))
    assert(fuertes(registro, 5.2).contains("Valle"))
  }

  test("P1-3 fuertes: sobre registro vacío da lista vacía") {
    assert(fuertes(vacio, 0.0) == List())
  }

  test("P1-4 enRegion: región con varios sismos devuelve todas sus magnitudes en orden") {
    assert(enRegion(registro, "Choco") == List(6.0, 3.1))
  }

  test("P1-5 enRegion: distingue mayúsculas/minúsculas (no hay 'valle')") {
    assert(enRegion(registro, "valle") == List())
  }

  test("P1-6 paresCercanos: dif = 0.0 solo empareja magnitudes idénticas") {
    assert(paresCercanos(registro, 0.0) == List())
  }

  test("P1-7 paresCercanos: umbral grande empareja todas las regiones") {
    assert(paresCercanos(registro, 100.0).length == 3)
  }

  test("P1-8 paresCercanos: una sola región no produce parejas") {
    val soloValle = List(Sismo(Fecha(2025, 1), "Valle", 4.0, 10))
    assert(paresCercanos(soloValle, 10.0) == List())
  }

  // Punto 2 -------------------------------------------------------------------

  test("porRegion: agrupa los siete sismos en tres regiones") {
    assert(porRegion(registro).keySet == Set("Valle", "Santander", "Choco"))
    assert(porRegion(registro).values.map(_.length).sum == 7)
  }

  test("conteoPorRegion: tres, dos y dos") {
    assert(conteoPorRegion(registro) ==
      Map("Valle" -> 3, "Santander" -> 2, "Choco" -> 2))
  }

  test("magnitudMaxima: la mayor de cada región") {
    assert(magnitudMaxima(registro) ==
      Map("Valle" -> 5.3, "Santander" -> 5.1, "Choco" -> 6.0))
  }

  test("conteoPorRegion: el registro vacío da el mapa vacío") {
    assert(conteoPorRegion(vacio) == Map())
  }

  test("magnitudMaxima: las regiones ausentes no aparecen") {
    assert(!magnitudMaxima(registro).contains("Amazonas"))
  }

  // Punto 2 Propio ------------------------------------------------------------

  test("P2-1 porRegion: cada sismo queda en su región, sin perder ninguno") {
    val m = porRegion(registro)
    assert(m("Valle").length == 3)
    assert(m("Santander").length == 2)
    assert(m("Choco").length == 2)
    assert(m.values.flatten.toList.sortBy(x => (x.region, x.magnitud)) ==
      registro.sortBy(x => (x.region, x.magnitud)))
  }

  test("P2-2 porRegion: registro de un solo sismo") {
    val uno = List(Sismo(Fecha(2025, 1), "Cauca", 3.5, 10))
    assert(porRegion(uno) == Map("Cauca" -> uno))
  }

  test("P2-3 conteoPorRegion: una región con tres sismos cuenta tres") {
    assert(conteoPorRegion(registro)("Valle") == 3)
  }

  test("P2-4 conteoPorRegion: región con un único sismo cuenta uno") {
    val uno = List(Sismo(Fecha(2025, 2), "Nariño", 4.1, 20))
    assert(conteoPorRegion(uno) == Map("Nariño" -> 1))
  }

  test("P2-5 magnitudMaxima: con una sola región de un sismo coincide con su magnitud") {
    val uno = List(Sismo(Fecha(2025, 2), "Nariño", 4.1, 20))
    assert(magnitudMaxima(uno) == Map("Nariño" -> 4.1))
  }

  test("P2-6 magnitudMaxima: el máximo no confunde con el último de la lista") {
    assert(magnitudMaxima(registro)("Choco") == 6.0)
  }

  // Punto 3 -------------------------------------------------------------------

  test("conteoSeguro: una región desconocida responde cero") {
    assert(conteoSeguro(registro)("Amazonas") == 0)
  }

  test("conteoSeguro: una región conocida responde su conteo") {
    assert(conteoSeguro(registro)("Valle") == 3)
  }

  test("conteoSeguro: sobre el registro vacío todo responde cero") {
    assert(conteoSeguro(vacio)("Valle") == 0)
  }

  test("maximaDe: el Chocó llega a 6.0") {
    assert(maximaDe(registro, "Choco") == Some(6.0))
  }

  test("maximaDe: una región sin registros da vacío") {
    assert(maximaDe(registro, "Amazonas") == None)
  }

  test("regionMasActiva: el Valle, con tres sismos") {
    assert(regionMasActiva(registro) == Some("Valle"))
  }

  test("regionMasActiva: sobre el registro vacío da vacío") {
    assert(regionMasActiva(vacio) == None)
  }

  test("regionMasActiva: en empate gana la primera alfabéticamente") {
    val empate = List(
      Sismo(Fecha(2025, 1), "Zulia", 4.0, 10),
      Sismo(Fecha(2025, 1), "Arauca", 4.0, 10))
    assert(regionMasActiva(empate) == Some("Arauca"))
  }

  // Punto 3 Propio ------------------------------------------------------------

  test("P3-1 conteoSeguro: dos consultas a regiones desconocidas dan cero") {
    val m = conteoSeguro(registro)
    assert(m("Amazonas") == 0)
    assert(m("Atlántico") == 0)
  }

  test("P3-2 conteoSeguro: no altera los conteos de las regiones conocidas") {
    val m = conteoSeguro(registro)
    assert(m("Valle") == 3)
    assert(m("Santander") == 2)
    assert(m("Choco") == 2)
  }

  test("P3-3 maximaDe: región con un único sismo devuelve Some de su magnitud") {
    val uno = List(Sismo(Fecha(2025, 2), "Cauca", 4.7, 15))
    assert(maximaDe(uno, "Cauca") == Some(4.7))
  }

  test("P3-4 maximaDe: distingue mayúsculas/minúsculas") {
    assert(maximaDe(registro, "choco") == None)
    assert(maximaDe(registro, "Choco") == Some(6.0))
  }

  test("P3-5 regionMasActiva: empate entre dos regiones gana la alfabéticamente menor") {
    val empate = List(
      Sismo(Fecha(2025, 1), "Tolima", 4.0, 10),
      Sismo(Fecha(2025, 1), "Boyacá", 4.5, 10))
    assert(regionMasActiva(empate) == Some("Boyacá"))
  }

  test("P3-6 regionMasActiva: empate entre tres regiones gana la primera alfabética") {
    val empate3 = List(
      Sismo(Fecha(2025, 1), "Zulia", 4.0, 10),
      Sismo(Fecha(2025, 1), "Arauca", 4.0, 10),
      Sismo(Fecha(2025, 1), "Meta", 4.0, 10))
    assert(regionMasActiva(empate3) == Some("Arauca"))
  }

  // Punto 4 -------------------------------------------------------------------

  test("replicas: las primeras cuatro de un sismo de 5.0") {
    assert(replicas(5.0).take(4).toList == List(5.0, 4.0, 3.2, 2.56))
  }

  test("replicas: la secuencia es infinita y no se cuelga") {
    assert(replicas(5.0).take(200).toList.length == 200)
  }

  test("replicas: empieza en la magnitud dada") {
    assert(replicas(7.3).head == 7.3)
  }

  test("replicas: los valores van redondeados a dos decimales") {
    assert(replicas(5.0).take(6).toList.forall(x => x == Math.round(x * 100.0) / 100.0))
  }

  test("replicasSensibles: las que superan 3.0 partiendo de 5.0") {
    assert(replicasSensibles(5.0, 3.0) == List(5.0, 4.0, 3.2))
  }

  test("replicasSensibles: si la primera no supera el mínimo, no hay ninguna") {
    assert(replicasSensibles(2.0, 3.0) == List())
  }

  test("replicasSensibles: el mínimo es estricto") {
    assert(replicasSensibles(3.0, 3.0) == List())
  }

  // Punto 4 Propio ------------------------------------------------------------

  test("P4-1 replicas: desde 1.0 los primeros cuatro términos") {
    assert(replicas(1.0).take(4).toList == List(1.0, 0.8, 0.64, 0.51))
  }

  test("P4-2 replicas: desde 0.0 se queda en 0.0") {
    assert(replicas(0.0).take(5).toList == List(0.0, 0.0, 0.0, 0.0, 0.0))
  }

  test("P4-3 replicas: take(1000) no se cuelga (perezosa)") {
    assert(replicas(7.3).take(1000).toList.length == 1000)
  }

  test("P4-4 replicas: con mínimo 0.0 se acota con take sobre la secuencia perezosa") {
    // takeWhile y take sobre LazyList son perezosos; la secuencia se estanca en 0.02 y nunca baja de 0.
    val primeras = replicas(5.0).takeWhile(_ > 0.0).take(5).toList
    assert(primeras == List(5.0, 4.0, 3.2, 2.56, 2.05))
  }

  test("P4-5 replicasSensibles: mínimo exactamente igual al segundo término corta ahí") {
    assert(replicasSensibles(5.0, 4.0) == List(5.0))
  }

  test("P4-6 replicasSensibles: mínimo mayor que la primera magnitud no devuelve nada") {
    assert(replicasSensibles(5.0, 6.0) == List())
  }

  test("P4-7 replicasSensibles: desde 1.0 con mínimo 0.5 corta en 0.51") {
    assert(replicasSensibles(1.0, 0.5) == List(1.0, 0.8, 0.64, 0.51))
  }

  // Punto 5 -------------------------------------------------------------------

  test("informe: el resumen de las tres regiones") {
    assert(informe(registro, 3.0) ==
      Map("Valle" -> Resumen(3, 5.3, 3),
        "Santander" -> Resumen(2, 5.1, 3),
        "Choco" -> Resumen(2, 6.0, 4)))
  }

  test("informe: el registro vacío da el mapa vacío") {
    assert(informe(vacio, 3.0) == Map())
  }

  test("informe: el conteo coincide con conteoPorRegion") {
    assert(informe(registro, 3.0).map { case (r, res) => (r, res.sismos) } ==
      conteoPorRegion(registro))
  }

  test("informe: la máxima coincide con magnitudMaxima") {
    assert(informe(registro, 3.0).map { case (r, res) => (r, res.maxima) } ==
      magnitudMaxima(registro))
  }

  test("informe: las réplicas coinciden con replicasSensibles del más fuerte") {
    assert(informe(registro, 3.0)("Choco").replicas ==
      replicasSensibles(6.0, 3.0).length)
  }

  // Punto 5 Propio ------------------------------------------------------------

  test("P5-1 informe: con mínimo alto, ninguna región tiene réplicas sensibles") {
    val m = informe(registro, 100.0)
    assert(m.values.forall(_.replicas == 0))
  }

  test("P5-2 informe: región de un único sismo tiene conteo 1 y réplicas correctas") {
    val uno = List(Sismo(Fecha(2025, 2), "Cauca", 3.2, 15))
    assert(informe(uno, 2.5) == Map("Cauca" -> Resumen(1, 3.2, 2)))
  }

  test("P5-3 informe: el conteo de Valle es 3, independiente del mínimo") {
    assert(informe(registro, 0.5)("Valle").sismos == 3)
  }

  test("P5-4 informe: la máxima de cada región es la de magnitudMaxima") {
    val m = informe(registro, 2.0)
    assert(m("Valle").maxima == 5.3)
    assert(m("Santander").maxima == 5.1)
    assert(m("Choco").maxima == 6.0)
  }

  test("P5-5 informe: con mínimo 4.0, las réplicas cambian respecto a mínimo 3.0") {
    assert(informe(registro, 4.0)("Choco").replicas == 2)
  }

  test("P5-6 informe: registro vacío da mapa vacío con cualquier mínimo") {
    assert(informe(vacio, 0.0) == Map())
    assert(informe(vacio, 5.0) == Map())
  }
}