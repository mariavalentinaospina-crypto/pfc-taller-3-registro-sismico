package taller

/**
 * Taller 3 — consultas sobre un registro sísmico con expresiones for, mapas y
 * secuencias perezosas.
 */
class RegistroSismico {

  type Registro = List[Sismo]

  // Punto 1: consultas con expresiones for ------------------------------------

  /** Las regiones de los sismos que superan el umbral, en orden y con repetidos. */
  def fuertes(r: Registro, umbral: Double): List[String] =
    for (s <- r if s.magnitud > umbral) yield s.region

  /** Las magnitudes registradas en una región. */
  def enRegion(r: Registro, region: String): List[Double] =
    for (s <- r if s.region == region) yield s.magnitud

  /** Parejas de regiones distintas con algún sismo de magnitudes cercanas. */
  def paresCercanos(r: Registro, dif: Double): List[(String, String)] = {
    val regiones = (for (s <- r) yield s.region).distinct

    def cercanas(a: String, b: String): Boolean = {
      val magsA = for (x <- r if x.region == a) yield x.magnitud
      val magsB = for (y <- r if y.region == b) yield y.magnitud
      (for {
        ma <- magsA
        mb <- magsB
        if math.abs(ma - mb) <= dif
      } yield (ma, mb)).nonEmpty
    }

    for {
      (a, i) <- regiones.zipWithIndex
      b       <- regiones.drop(i + 1)
      if cercanas(a, b)
    } yield (a, b)
  }

  // Punto 2: agrupar con mapas ------------------------------------------------

  def porRegion(r: Registro): Map[String, List[Sismo]] =
    r.groupBy(_.region)

  def conteoPorRegion(r: Registro): Map[String, Int] =
    r.groupBy(_.region).view.mapValues(_.length).toMap

  def magnitudMaxima(r: Registro): Map[String, Double] =
    r.groupBy(_.region).view.mapValues(_.map(_.magnitud).max).toMap

  // Punto 3: consultas seguras ------------------------------------------------

  /** Un mapa que responde cero por las regiones que no están en el registro. */
  def conteoSeguro(r: Registro): Map[String, Int] =
    conteoPorRegion(r).withDefaultValue(0)

  /** La magnitud máxima de una región, o vacío si no hay registros suyos. */
  def maximaDe(r: Registro, region: String): Option[Double] =
    r.filter(_.region == region).map(_.magnitud) match {
      case Nil => None
      case ms  => Some(ms.max)
    }

  /** La región con más sismos; en empate, la primera en orden alfabético. */
  def regionMasActiva(r: Registro): Option[String] =
    if (r.isEmpty) None
    else {
      val conteos = conteoPorRegion(r)
      val max = conteos.values.max
      Some(conteos.filter(_._2 == max).keys.min)
    }

  // Punto 4: réplicas perezosas -----------------------------------------------

  def redondear(x: Double): Double = Math.round(x * 100.0) / 100.0

  /** La secuencia infinita que empieza en la magnitud y multiplica por 0.8. */
  def replicas(magnitud: Double): LazyList[Double] =
    LazyList.iterate(redondear(magnitud))(x => redondear(x * 0.8))

  /** Las réplicas mientras superen el mínimo, incluida la primera. */
  def replicasSensibles(magnitud: Double, minimo: Double): List[Double] =
    replicas(magnitud).takeWhile(_ > minimo).toList

  // Punto 5: informe combinado ------------------------------------------------

  /** Conteo, magnitud máxima y réplicas esperadas del sismo más fuerte, por región. */
  def informe(r: Registro, minimo: Double): Map[String, Resumen] =
    r.groupBy(_.region).view.mapValues { sismos =>
      val max = sismos.map(_.magnitud).max
      Resumen(
        sismos   = sismos.length,
        maxima   = max,
        replicas = replicasSensibles(max, minimo).length
      )
    }.toMap
}