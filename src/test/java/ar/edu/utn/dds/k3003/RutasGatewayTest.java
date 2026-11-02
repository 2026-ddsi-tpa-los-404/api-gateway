package ar.edu.utn.dds.k3003;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

// Cada módulo se reemplaza por un servidor local que responde "<modulo> <path recibido>",
// así se verifica a dónde llega cada request y con qué path.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RutasGatewayTest {

  private static final String[] MODULOS = {"donaciones", "logistica", "donadores-entidades", "incentivos", "consultas"};
  private static final HttpServer[] servidores = new HttpServer[MODULOS.length];

  @Autowired
  private WebTestClient cliente;

  @DynamicPropertySource
  static void urlsDeLosModulos(DynamicPropertyRegistry registry) throws IOException {
    for (int i = 0; i < MODULOS.length; i++) {
      String modulo = MODULOS[i];
      HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      servidor.createContext("/", intercambio -> {
        String uri = intercambio.getRequestURI().toString();
        byte[] cuerpo = (modulo + " " + intercambio.getRequestMethod() + " " + uri).getBytes(StandardCharsets.UTF_8);
        intercambio.sendResponseHeaders(200, cuerpo.length);
        intercambio.getResponseBody().write(cuerpo);
        intercambio.close();
      });
      servidor.start();
      servidores[i] = servidor;

      String url = "http://localhost:" + servidor.getAddress().getPort();
      String variable = modulo.toUpperCase().replace('-', '_') + "_URL";
      registry.add(variable, () -> url);
    }
  }

  @AfterAll
  static void apagarServidores() {
    for (HttpServer servidor : servidores) {
      if (servidor != null) servidor.stop(0);
    }
  }

  @Test
  void enrutaDonacionesQuitandoElPrefijo() {
    esperar("GET", "/api/donaciones/donaciones/5", "donaciones GET /donaciones/5");
  }

  @Test
  void enrutaLogistica() {
    esperar("POST", "/api/logistica/depositos/3/matchmaking", "logistica POST /depositos/3/matchmaking");
  }

  @Test
  void enrutaDonadoresYEntidadesConQueryString() {
    esperar("GET", "/api/donadores-entidades/necesidades?tipo=RECURRENTE", "donadores-entidades GET /necesidades?tipo=RECURRENTE");
  }

  // /donadores existe en Donadores y Entidades y en Incentivos: el prefijo decide a cuál va.
  @Test
  void enrutaIncentivosAunqueElPathCoincidaConOtroModulo() {
    esperar("GET", "/api/incentivos/donadores/12/mision", "incentivos GET /donadores/12/mision");
  }

  @Test
  void enrutaConsultas() {
    esperar("GET", "/api/consultas/donadores/12/ficha", "consultas GET /donadores/12/ficha");
  }

  @Test
  void unPathSinPrefijoDeModuloDa404() {
    cliente.get().uri("/donaciones/5").exchange().expectStatus().isNotFound();
  }

  @Test
  void exponeElHealth() {
    cliente.get().uri("/actuator/health").exchange().expectStatus().isOk();
  }

  private void esperar(String metodo, String path, String respuestaEsperada) {
    cliente.method(org.springframework.http.HttpMethod.valueOf(metodo)).uri(path)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class).isEqualTo(respuestaEsperada);
  }
}
