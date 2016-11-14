package edu.unq.pconc.gameoflife.solution;

import edu.unq.pconc.gameoflife.CellGrid;
import edu.unq.pconc.gameoflife.solution.celda.Celda;
import edu.unq.pconc.gameoflife.solution.celda.CeldaMuerta;
import edu.unq.pconc.gameoflife.solution.celda.CeldaViva;
import edu.unq.pconc.gameoflife.solution.exceptions.LaCoordenadaCaeFueraDelTableroException;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.Math.abs;

public class GameOfLifeGrid implements CellGrid {

    private Map<Coordenada, Celda> tablero;
    private List<ThreadRunner> threads;
    private int ancho;
    private int alto;
    private int generations;
    private int threadActual;

    public GameOfLifeGrid(int threads, int ancho, int alto){
        this.threads = crearThreads(threads);
        this.threadActual = 0;
        this.ancho = ancho;
        this.alto = alto;
        this.tablero = configuracionInicial(ancho, alto);
    }

    public GameOfLifeGrid(){
        generations = 0;
        ancho = 0;
        alto = 0;
        threads = new ArrayList<>();
        threadActual = 0;
        tablero = new Hashtable<>();
    }

    private List<ThreadRunner> crearThreads(int cantidadDeThreads) {
        List<ThreadRunner> threads = new ArrayList<>();
        IntStream.range(0, cantidadDeThreads).forEach(valor -> threads.add(new ThreadRunner()));
        return threads;
    }

    private Map<Coordenada, Celda> configuracionInicial(int ancho, int alto) {
        Map<Coordenada, Celda> tablero = new Hashtable<>();

        IntStream.range(0, ancho).forEach(coordenadaAncho -> {
            IntStream.range(0, alto).forEach(coordenadaAlto -> {
                tablero.put(new Coordenada(coordenadaAncho, coordenadaAlto), new CeldaMuerta(coordenadaAncho, coordenadaAlto));
            });
        });
        return tablero;
    }

    @Override
    public boolean getCell(int col, int row) {
        boolean cell = getCeldaEnCoordenada(col, row).estaViva();
        return cell;
    }

    @Override
    public synchronized void setCell(int col, int row, boolean estadoASetear) {
        boolean estadoExistente = this.getCell(col, row);
        if(estadoASetear && !estadoExistente){
            revivirCelda(col, row);
        }
        if(!estadoASetear && estadoExistente){
            matarCelda(col, row);
        }
    }

    private void matarCelda(int col, int row) {
        this.tablero.put(new Coordenada(col, row), new CeldaMuerta(col, row));
    }

    private void revivirCelda(int col, int row) {
        this.tablero.put(new Coordenada(col, row), new CeldaViva(col, row));
    }

    public Celda getCeldaEnCoordenada(int col, int row) {
        return busquedaDeCoordenada(col, row).orElseThrow(() -> new LaCoordenadaCaeFueraDelTableroException(col, row));
    }

    private Optional<Celda> busquedaDeCoordenada(int col, int row) {
        return Optional.ofNullable(this.tablero.get(new Coordenada(col, row)));
    }

    @Override
    public Dimension getDimension() {
        return new Dimension(ancho, alto);
    }

    @Override
    public synchronized void resize(int ancho, int alto) {
        int faltantesAncho = abs(ancho - this.ancho);
        int faltantesAlto = abs(alto - this.alto);

        if(ancho > this.ancho || alto > this.alto){
            IntStream.range(0, faltantesAncho).forEach(x -> {
                IntStream.range(0, faltantesAlto).forEach(y -> {
                    int newX = this.ancho + x;
                    int newY = this.alto + y;
                    this.tablero.put(new Coordenada(newX, newY), new CeldaMuerta(newX, newY));
                });
            });
        }

        if(ancho < this.ancho || alto < this.alto){
            IntStream.range(0, faltantesAncho).forEach(x -> {
                IntStream.range(0, faltantesAlto).forEach(y -> {
                    int newX = this.ancho - x;
                    int newY = this.alto - y;
                    this.tablero.remove(new Coordenada(newX, newY));
                });
            });
        }

        this.ancho = ancho;
        this.alto = alto;
    }

    @Override
    public synchronized void setThreads(int cantidadDeThreads) {
        this.threads = crearThreads(cantidadDeThreads);
        this.threadActual = 0;
    }

    @Override
    public void clear() {
        this.tablero = this.configuracionInicial(this.ancho, this.alto);
    }

    @Override
    public int getGenerations() {
        return generations;
    }

    @Override
    public synchronized void next() {
        Map<Coordenada, Celda> configuracionNueva = new Hashtable<>();

        this.paralelizarNext(configuracionNueva);

        this.tablero = configuracionNueva;
        this.generations++;
    }

    private void paralelizarNext(Map<Coordenada, Celda> configuracionNueva) {
        tablero.values().forEach(celda -> this.nextThread().add(celda));
        threads.stream().forEach(thread -> thread.start(this, configuracionNueva));
        threads.forEach(ThreadRunner::joinThread);
        threads.forEach(ThreadRunner::cleanList);
    }

    private ThreadRunner nextThread() {
        this.threadActual = this.threadActual + 1;
        if(this.threadActual >= threads.size()){
            threadActual = 0;
        }
        return threads.get(threadActual);
    }

    public int getThreadsSize() {
        return threads.size();
    }
}