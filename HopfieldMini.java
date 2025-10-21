import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class HopfieldMini {

    // ======== UTILIDADES DE PATRONES ('.'=-1, '#'=+1) ========
    static int[] gridToVec(String[] g) {
        int H = g.length, W = g[0].length();
        int[] v = new int[H * W];
        int k = 0;
        for (String row : g) {
            if (row.length() != W) throw new IllegalArgumentException("Filas con distinto ancho");
            for (char c : row.toCharArray()) v[k++] = (c == '#') ? 1 : -1;
        }
        return v;
    }

    static String[] vecToGrid(int[] v, int H, int W) {
        String[] g = new String[H];
        int k = 0;
        for (int i = 0; i < H; i++) {
            StringBuilder sb = new StringBuilder(W);
            for (int j = 0; j < W; j++) sb.append(v[k++] == 1 ? '#' : '.');
            g[i] = sb.toString();
        }
        return g;
    }

    static void printGrid(String title, String[] g) {
        System.out.println(title);
        for (String r : g) System.out.println(r);
        System.out.println();
    }

    static int[] addFlipNoise(int[] v, double p) {
        int[] out = Arrays.copyOf(v, v.length);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < out.length; i++) if (rnd.nextDouble() < p) out[i] = -out[i];
        return out;
    }

    // ======== RED DE HOPFIELD (mínima) ========
    static class Hopfield {
        final int n;
        final double[][] W;

        Hopfield(int n) { this.n = n; this.W = new double[n][n]; }

        // Entrenamiento hebbiano con 1 patrón: W = p p^T, diag=0
        void train(int[] p) {
            for (int i = 0; i < n; i++) Arrays.fill(W[i], 0.0);
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
                if (i != j) W[i][j] = p[i] * p[j];
        }

        // Actualización SÍNCRONA: s <- sign(W s)
        int[] recallSync(int[] init, int maxIters) {
            int[] s = Arrays.copyOf(init, n);
            for (int it = 0; it < maxIters; it++) {
                int[] ns = new int[n];
                for (int i = 0; i < n; i++) {
                    double h = 0.0;
                    for (int j = 0; j < n; j++) h += W[i][j] * s[j];
                    ns[i] = (h > 0) ? 1 : (h < 0 ? -1 : s[i]); // si h=0, conservar
                }
                if (Arrays.equals(ns, s)) break;
                s = ns;
            }
            return s;
        }
    }

    // ======== DEMO ========
    public static void main(String[] args) {
        // Patrón 8x8: anillo simple (zona de trabajo visible)
        String[] idealGrid = {
            "........",
            ".######.",
            ".#....#.",
            ".#....#.",
            ".#....#.",
            ".#....#.",
            ".######.",
            "........"
        };

        int H = idealGrid.length, W = idealGrid[0].length();
        int[] ideal = gridToVec(idealGrid);

        Hopfield net = new Hopfield(H * W);
        net.train(ideal); // entreno con UN patrón (memoria asociativa mínima)

        // Creo una observación con 8% de ruido (flips)
        int[] noisy = addFlipNoise(ideal, 0.08);

        // Recupero con actualización síncrona
        int[] rec = net.recallSync(noisy, 50);

        // Muestro resultados
        printGrid("PATRÓN IDEAL:", idealGrid);
        printGrid("OBSERVACIÓN (RUIDO):", vecToGrid(noisy, H, W));
        printGrid("RECUPERADO:", vecToGrid(rec, H, W));

        // Métrica rápida de acierto
        double acc = match(ideal, rec) * 100.0;
        System.out.printf("Coincidencia con ideal: %.2f%%%n", acc);
    }

    static double match(int[] a, int[] b) {
        int ok = 0; for (int i = 0; i < a.length; i++) if (a[i] == b[i]) ok++;
        return ok / (double) a.length;
    }
}
