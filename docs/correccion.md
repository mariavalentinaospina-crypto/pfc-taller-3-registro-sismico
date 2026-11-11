# Informe de corrección: Taller 3, registro sísmico

Fundamentos de Programación Funcional y Concurrente
Escuela de Ingeniería de Sistemas y Computación, Universidad del Valle

En este informe argumentamos que cada función de `RegistroSismico.scala` cumple su especificación. Usamos inducción estructural sobre listas para las consultas con `for` y para la secuencia de réplicas. Para los mapas nos apoyamos en la especificación de `groupBy`. Al final explicamos por qué ningún resultado depende del orden en que se recorra el registro.

## Notación

- $r$ es un registro, es decir, una `List[Sismo]`.
- $\text{reg}(s)$ y $\text{mag}(s)$ son la región y la magnitud de un sismo $s$.
- $L = [x_1, x_2, \ldots, x_n]$ es una lista; $x :: L'$ es la lista con cabeza $x$ y cola $L'$; $[\,]$ es la lista vacía.
- $L_1 \mathbin{+\!\!+} L_2$ es la concatenación de listas.
- $\text{ms}(L)$ es el multiconjunto de los elementos de $L$ (cuenta repeticiones, ignora el orden).
- $[\,e(x) \mid x \in L,\ P(x)\,]$ es la lista de los $e(x)$ con $x$ recorriendo $L$ en orden y cumpliendo $P$.
- $\rho(x)$ es `redondear(x)`, es decir, $\text{round}(100x)/100$.

## 1. Consultas con `for`

### 1.1. Lema de los `for` con un generador

**Lema F.** Para toda lista $L$, predicado $P$ y función $e$:

$$
\text{for } (x \leftarrow L \text{ if } P(x)) \text{ yield } e(x) \;=\; [\,e(x) \mid x \in L,\ P(x)\,]
$$

*Demostración por inducción sobre $L$.* El `for` se traduce a $L.\text{withFilter}(P).\text{map}(e)$.

- **Caso base,** $L = [\,]$: no hay elementos que filtrar ni proyectar, así que el resultado es $[\,]$, que coincide con $[\,e(x) \mid x \in [\,],\ P(x)\,] = [\,]$.
- **Caso inductivo,** $L = x :: L'$. Por **hipótesis de inducción (HI)** el `for` sobre $L'$ es $[\,e(y) \mid y \in L',\ P(y)\,]$.
  - Si $P(x)$, el `for` sobre $L$ es $e(x) :: [\,e(y) \mid y \in L',\ P(y)\,]$.
  - Si no, es solo $[\,e(y) \mid y \in L',\ P(y)\,]$.

  En ambos casos coincide con la definición de $[\,e(x) \mid x \in L,\ P(x)\,]$. $\blacksquare$

### 1.2. `fuertes` y `enRegion`

```scala
def fuertes(r: Registro, umbral: Double): List[String] =
  for (s <- r if s.magnitud > umbral) yield s.region

def enRegion(r: Registro, region: String): List[Double] =
  for (s <- r if s.region == region) yield s.magnitud
```

**Especificación:**

$$
\text{fuertes}(r, u) = [\,\text{reg}(s) \mid s \in r,\ \text{mag}(s) > u\,]
\qquad
\text{enRegion}(r, g) = [\,\text{mag}(s) \mid s \in r,\ \text{reg}(s) = g\,]
$$

**Corrección:** son exactamente el Lema F con $P(s) \equiv \text{mag}(s) > u$ y $e(s) = \text{reg}(s)$ en el primer caso, y con $P(s) \equiv \text{reg}(s) = g$ y $e(s) = \text{mag}(s)$ en el segundo. El umbral es estricto porque $P$ usa $>$. Si ninguna región cumple, el resultado es $[\,]$. $\blacksquare$

### 1.3. `maximaDe`

```scala
def maximaDe(r: Registro, region: String): Option[Double] =
  enRegion(r, region).maxOption
```

**Especificación:**

$$
\text{maximaDe}(r, g) =
\begin{cases}
\text{None} & \text{si } \{\,s \in r \mid \text{reg}(s) = g\,\} = \emptyset \\
\text{Some}\bigl(\max\,\{\,\text{mag}(s) \mid s \in r,\ \text{reg}(s) = g\,\}\bigr) & \text{en otro caso}
\end{cases}
$$

**Demostración.** Sea $M = \text{enRegion}(r, g)$. Por la sección 1.2, $M$ es la lista de las magnitudes de los sismos de la región $g$. Por definición, `maxOption` devuelve `None` si $M = [\,]$ y `Some` del mayor elemento de $M$ si no. Que $M$ sea vacía equivale a que no haya sismos de $g$ en $r$, y el mayor elemento de $M$ es el máximo del conjunto de magnitudes de la especificación. $\blacksquare$

### 1.4. `paresCercanos`

```scala
def paresCercanos(r: Registro, dif: Double): List[(String, String)] = {
  val regiones = (for (s <- r) yield s.region).distinct

  def cercanas(a: String, b: String): Boolean = {
    val magsA = enRegion(r, a)
    val magsB = enRegion(r, b)
    val parejas = for {
      ma <- magsA
      mb <- magsB
      if math.abs(ma - mb) <= dif
    } yield (ma, mb)
    parejas.nonEmpty
  }

  for {
    (a, i) <- regiones.zipWithIndex
    b <- regiones.drop(i + 1)
    if cercanas(a, b)
  } yield (a, b)
}
```

**Especificación:** el resultado contiene las parejas $(a, b)$ de regiones distintas tales que

$$
C(a, b) \;\equiv\; \exists\, x, y \in r :\ \text{reg}(x) = a,\ \text{reg}(y) = b,\ |\text{mag}(x) - \text{mag}(y)| \leq \text{dif}
$$

cada pareja no ordenada sale una sola vez, y las regiones van en el orden en que aparecen en $r$.

**Paso 1: `regiones`.** Por el Lema F, $\text{for}\ (s \leftarrow r)\ \text{yield}\ \text{reg}(s)$ es la lista de las regiones de $r$ en orden, con repetidos. `distinct` conserva la primera aparición de cada una. Entonces $\text{regiones} = [g_1, \ldots, g_k]$ tiene todas las regiones de $r$, sin repetir, en orden de primera aparición.

**Paso 2: `cercanas(a, b)` es verdadera si y solo si $C(a, b)$.** Por el Lema F, $\text{magsA}$ y $\text{magsB}$ son las listas de magnitudes de las regiones $a$ y $b$. El `for` de dos generadores se traduce a $\text{magsA}.\text{flatMap}(ma \mapsto \text{magsB}.\text{withFilter}(\ldots).\text{map}(\ldots))$, y `flatMap` concatena los resultados de cada $ma$. Así, `parejas` es la lista de todos los $(ma, mb)$ con $ma \in \text{magsA}$, $mb \in \text{magsB}$ y $|ma - mb| \leq \text{dif}$. Esa lista es no vacía exactamente cuando existen $x, y$ con las condiciones de $C(a, b)$.

**Paso 3: el `for` externo.** Se traduce a un `flatMap` sobre $(g_i, i)$, que concatena, para $i = 1, \ldots, k$ y en ese orden, las listas $[\,(g_i, g_j) \mid j > i,\ C(g_i, g_j)\,]$ (aquí `drop(i + 1)` en la numeración desde 0 deja justo los $g_j$ con $j > i$). Entonces

$$
\text{resultado} = [\,(g_i, g_j) \mid 1 \leq i < j \leq k,\ C(g_i, g_j)\,]
$$

recorriendo los $(i, j)$ en orden lexicográfico.

**Propiedades que pide el enunciado:**

1. **Regiones distintas:** como los $g_i$ son todos distintos y $i < j$, $g_i \neq g_j$.
2. **Sin repetir:** cada par de índices $(i, j)$ con $i < j$ aparece una sola vez, así que cada pareja ordenada sale como mucho una vez. Además, $(g_j, g_i)$ nunca sale, porque exigiría $j < i$.
3. **Completitud:** sea $\{a, b\}$ con $a \neq b$ y $C(a, b)$. Ambas son regiones de $r$, así que $a = g_i$ y $b = g_j$ con $i \neq j$. Si $i < j$, la pareja $(a, b)$ sale. Si $j < i$, como $C$ es simétrica (porque $|u - v| = |v - u|$), también vale $C(b, a)$ y sale $(b, a)$. En ambos casos la pareja sale una vez.
4. **Orden:** cada pareja respeta el orden de primera aparición de sus regiones, porque $i < j$. $\blacksquare$

### 1.5. `replicasSensibles`

```scala
def redondear(x: Double): Double = Math.round(x * 100.0) / 100.0

def replicas(magnitud: Double): LazyList[Double] =
  LazyList.iterate(redondear(magnitud))(x => redondear(x * 0.8))

def replicasSensibles(magnitud: Double, minimo: Double): List[Double] =
  replicas(magnitud).takeWhile(_ > minimo).toList
```

**Especificación.** Sea $f(x) = \rho(0.8\,x)$ y la sucesión $x_0 = \rho(m)$, $x_{i+1} = f(x_i)$. Entonces

$$
\text{replicasSensibles}(m, \text{min}) = [\,x_0, x_1, \ldots, x_{k-1}\,]
$$

donde $k$ es el menor índice con $x_k \leq \text{min}$ (si no existe, la lista es infinita).

**Demostración.** Sea $G(x)$ el resultado de $\text{LazyList.iterate}(x)(f).\text{takeWhile}(\_ > \text{min})$. Por la definición de `iterate`, $\text{iterate}(x)(f) = x \mathbin{\#::} \text{iterate}(f(x))(f)$, y por la definición de `takeWhile`:

$$
G(x) =
\begin{cases}
[\,] & \text{si } x \leq \text{min} \\
x :: G(f(x)) & \text{si } x > \text{min}
\end{cases}
$$

Como $\text{replicasSensibles}(m, \text{min}) = G(\rho(m))$ (el `toList` solo pasa a lista lo que `takeWhile` produjo), basta ver que $G(x_0) = [x_0, \ldots, x_{k-1}]$. Lo hacemos por inducción sobre $k$.

- **Caso base,** $k = 0$: $x_0 \leq \text{min}$, así que $G(x_0) = [\,]$.
- **Caso inductivo,** $k > 0$: $x_0 > \text{min}$, así que $G(x_0) = x_0 :: G(x_1)$. La sucesión que empieza en $x_1$ es $x_1, x_2, \ldots$ y su primer índice con $x_i \leq \text{min}$ es $k - 1$ posiciones más adelante. Por **HI**, $G(x_1) = [x_1, \ldots, x_{k-1}]$, y entonces $G(x_0) = [x_0, \ldots, x_{k-1}]$. $\blacksquare$

**Terminación.** Trabajamos en centésimas: sea $n_i = 100\,x_i$, un entero, con $n_{i+1} = \text{round}(0.8\,n_i)$.

- Si $n_i \geq 3$: $0.8\,n_i \leq n_i - 1$ exactamente cuando $n_i \geq 5$, y para $n_i = 3, 4$ se cumple a mano ($\text{round}(2.4) = 2$ y $\text{round}(3.2) = 3$). Además $n_{i+1} \geq \text{round}(2.4) = 2$. Entonces, mientras $n_i \geq 3$, la sucesión baja al menos una centésima por paso, y nunca baja de $2$.
- Los puntos fijos son $n = 0, 1, 2$ (porque $\text{round}(0.8) = 1$, $\text{round}(1.6) = 2$ y $\text{round}(0) = 0$).

De aquí se sigue:

1. **Si $\text{min} \geq 0.02$:** la sucesión llega en a lo sumo $n_0$ pasos a un valor $\leq 0.02 \leq \text{min}$, así que $k$ existe, $G$ termina y la lista tiene a lo sumo $n_0$ elementos.
2. **Si $\text{min} < 0.02$ y $x_0 \geq 0.02$:** todos los $x_i \geq 0.02 > \text{min}$, así que $k$ no existe y `takeWhile` no termina. Es coherente con la especificación, que en ese caso describe una lista infinita. Para probarlo hay que acotar sobre la secuencia perezosa, por ejemplo con `replicas(5.0).takeWhile(_ > 0.0).take(5).toList`.

**Pereza.** Para producir los $k$ elementos solo se evalúan $x_0, \ldots, x_k$ (el último solo para comprobar que no cumple). Por eso el uso de memoria depende de $k$ y no de que la secuencia sea infinita.

## 2. Mapas

Para los mapas usamos la especificación documentada de `groupBy`: para una lista $L$ y una función $h$, $L.\text{groupBy}(h)$ es el mapa cuyas llaves son los valores $h(x)$ con $x \in L$, y cuyo valor en la llave $k$ es la lista $[\,x \mid x \in L,\ h(x) = k\,]$ (conserva el orden de $L$). No tiene más llaves.

### 2.1. `porRegion`, `conteoPorRegion` y `magnitudMaxima`

$$
\text{porRegion}(r)(g) = [\,s \mid s \in r,\ \text{reg}(s) = g\,], \qquad g \in \text{regiones}(r)
$$

Es directamente la especificación de `groupBy` con $h = \text{reg}$.

- **`conteoPorRegion`:** `mapValues(_.length)` reemplaza cada lista por su longitud, así que $\text{conteoPorRegion}(r)(g) = |\{s \in r \mid \text{reg}(s) = g\}|$.
- **`magnitudMaxima`:** cada grupo no es vacío (una llave solo existe si algún sismo la produce), así que `.map(_.magnitud).max` está bien definido y vale $\max\{\text{mag}(s) \mid s \in r,\ \text{reg}(s) = g\}$.

Como `groupBy` solo crea llaves de regiones presentes, las ausentes no aparecen, y para $r = [\,]$ los tres mapas son vacíos. `.toMap` solo fuerza el cálculo de la vista y no cambia el contenido. $\blacksquare$

### 2.2. `conteoSeguro`

`withDefaultValue(0)` no cambia las llaves ni sus valores: solo agrega un valor de respaldo para las llaves ausentes. Entonces $\text{conteoSeguro}(r)(g) = \text{conteoPorRegion}(r)(g)$ si $g$ está, y $0$ si no. Esto último coincide con la definición matemática, porque el conjunto de sismos de una región ausente tiene $0$ elementos. $\blacksquare$

### 2.3. `regionMasActiva`

**Especificación:** si $r = [\,]$, `None`. En otro caso, $\text{Some}(g^*)$ con

$$
g^* = \min\,\{\,g \mid \text{conteoPorRegion}(r)(g) = M\,\}, \qquad M = \max_g\ \text{conteoPorRegion}(r)(g)
$$

donde $\min$ es el menor en orden alfabético.

**Demostración.** Si $r = [\,]$, el programa devuelve `None` por el primer `if`. Si no, $r$ tiene al menos un sismo, así que `conteos` no es vacío y `conteos.values.max` es $M$. `filter(_._2 == mayor)` deja exactamente las regiones con conteo $M$, que es un conjunto no vacío (contiene la región que alcanza el máximo), y `keys.min` elige la menor alfabéticamente. Eso es $g^*$. $\blacksquare$

### 2.4. `informe`

```scala
def informe(r: Registro, minimo: Double): Map[String, Resumen] =
  porRegion(r).view.mapValues { sismos =>
    val maxima = sismos.map(_.magnitud).max
    Resumen(sismos.length, maxima, replicasSensibles(maxima, minimo).length)
  }.toMap
```

**Especificación:** para cada región $g$ de $r$, con $\text{grupo}_g = \{s \in r \mid \text{reg}(s) = g\}$,

$$
\text{informe}(r, \text{min})(g) = \text{Resumen}\bigl(|\text{grupo}_g|,\ \max_{s \in \text{grupo}_g} \text{mag}(s),\ |\text{replicasSensibles}(\max_{s \in \text{grupo}_g} \text{mag}(s), \text{min})|\bigr)
$$

**Demostración.** Por la sección 2.1, los grupos son exactamente $\text{grupo}_g$ y no son vacíos. En cada uno, `sismos.length` es $|\text{grupo}_g|$ y `maxima` es el máximo de sus magnitudes. El tercer campo es la longitud de `replicasSensibles` aplicada a ese máximo, que es correcta por la sección 1.5 (siempre que $\text{min} \geq 0.02$ para que termine). Las llaves son las regiones presentes, y para $r = [\,]$ el mapa es vacío. $\blacksquare$

## 3. Independencia del orden de recorrido

Decimos que una función no depende del orden de recorrido si, al permutar los sismos de $r$ (lo que equivale a recorrerlos en otro orden), el resultado no cambia, salvo por el orden que la propia especificación pide. Sea $r'$ una permutación cualquiera de $r$. Entonces $\text{ms}(r') = \text{ms}(r)$.

**Resultados que no cambian en absoluto.** Estos usan solo cardinalidades, máximos o mínimos de conjuntos definidos por una condición sobre cada sismo:

- **`conteoPorRegion`:** $|\{s \in r' \mid \text{reg}(s) = g\}| = |\{s \in r \mid \text{reg}(s) = g\}|$, porque ambos multiconjuntos de sismos son iguales y la cardinalidad no ve el orden.
- **`magnitudMaxima` y `maximaDe`:** el máximo de un multiconjunto de números es el mismo sin importar el orden, porque $\max$ es conmutativo y asociativo. La presencia o ausencia de una región también depende solo del multiconjunto.
- **`conteoSeguro`:** hereda de `conteoPorRegion`, porque `withDefaultValue(0)` no depende de $r$.
- **`regionMasActiva`:** $M$ es un máximo, y la región elegida es el mínimo alfabético de un conjunto que solo depende de los conteos. Por eso el desempate no depende del orden en que se recorra $r$.
- **`informe`:** sus tres campos son un conteo, un máximo y una función que solo depende de ese máximo y del mínimo dado. Entonces $\text{informe}(r', \text{min}) = \text{informe}(r, \text{min})$ como mapas.

**Resultados que cambian solo en lo que la especificación fija.**

- **`fuertes` y `enRegion`:** por el Lema F son listas definidas por un filtro y una proyección. Los elementos que contienen dependen solo del multiconjunto: $\text{ms}(\text{fuertes}(r', u)) = \text{ms}(\text{fuertes}(r, u))$, e igual para `enRegion`. El orden en que salen es el de $r$, y el enunciado lo exige ("en el orden en que aparecen").
- **`porRegion`:** las llaves son las mismas y cada grupo tiene los mismos sismos (igual como multiconjunto). Solo el orden interno de cada grupo sigue al de $r$.
- **`paresCercanos`:** la relación $C$ es simétrica ($|u - v| = |v - u|$) y se define con un $\exists$ sobre los sismos de dos regiones, que no depende del orden de $r$. Por la sección 1.4, el conjunto de parejas no ordenadas $\{a, b\}$ que salen es $\{\{a, b\} \mid a \neq b,\ C(a, b)\}$, y es igual para $r$ y $r'$. Lo único que cambia es qué región de cada pareja se escribe primero y el orden de la lista, que siguen el orden de primera aparición, como pide el enunciado.

**Funciones que no dependen de $r$.** `redondear`, `replicas` y `replicasSensibles` solo usan sus argumentos numéricos. Son funciones puras: el valor de $x_i$ está fijado por la recurrencia $x_{i+1} = \rho(0.8\,x_i)$, y la `LazyList` guarda cada elemento una vez calculado, así que todo el que lo pida obtiene el mismo valor, sin importar cuándo lo evalúe.

## 4. Conclusión

Argumentamos la corrección de las doce funciones del enunciado (`fuertes`, `enRegion`, `paresCercanos`, `porRegion`, `conteoPorRegion`, `magnitudMaxima`, `conteoSeguro`, `maximaDe`, `regionMasActiva`, `replicas`, `replicasSensibles` e `informe`) y de la función auxiliar `redondear`:

- **Consultas con `for`:** por el Lema F, demostrado por inducción sobre listas, y por la traducción de dos generadores a `flatMap`.
- **Réplicas perezosas:** por inducción sobre el índice $k$ de corte, con la condición exacta de terminación ($\text{min} \geq 0.02$).
- **Mapas:** por la especificación de `groupBy`, de la que se derivan los conteos, los máximos, el valor por defecto, el desempate y el informe.
- **Independencia del orden:** los resultados que dependen solo de cardinalidades, máximos o mínimos no cambian al permutar $r$, y los demás solo cambian en el orden que la propia especificación fija.