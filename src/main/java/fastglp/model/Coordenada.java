package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fastglp.utils.Utils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
@Getter @Setter
@NoArgsConstructor
@Embeddable
public class Coordenada implements Comparable<Coordenada>{
    @Column(nullable = false)
    private double x;
    @Column(nullable = false)
    private double y;

    public Coordenada(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int compareTo(Coordenada other) {
        if (this.x != other.x) {
            return Double.compare(this.x, other.x);
        } else {
            return Double.compare(this.y, other.y);
        }
    }

    @Override
    public String toString() {
        //con 1 decimal si es decimal, si es enterio sin dedimales
        return "("+ Utils.formatDouble(this.x)+","+Utils.formatDouble(this.y)+")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordenada that)) return false;
        return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }

    //distancia entre dos coordenadas
    public double distancia(Coordenada c){
        return Math.abs(this.x-c.x)+Math.abs(this.y-c.y);
    }
    public Coordenada derecha(){
        //si es entero sumar 1, si no es entero aproximar al entero mas cercano
        return new Coordenada(Math.floor(this.x)+1,this.y);
    }
    public Coordenada izquierda(){
        //si es entero sumar 1, si no es entero aproximar al entero mas cercano
        return new Coordenada(Math.ceil(this.x)-1,this.y);
    }
    public Coordenada arriba(){
        //si es entero sumar 1, si no es entero aproximar al entero mas cercano
        return new Coordenada(this.x,Math.floor(this.y)+1);
    }
    public Coordenada abajo(){
        //si es entero sumar 1, si no es entero aproximar al entero mas cercano
        return new Coordenada(this.x,Math.ceil(this.y)-1);
    }
    @JsonIgnore
    public boolean isXInteger(){
        return  this.x==(int)this.x;
    }
    @JsonIgnore
    public boolean isYInteger(){
        return this.y==(int)this.y;
    }
    @JsonIgnore
    public boolean isInteger(){
        return this.isXInteger() && this.isYInteger();
    }
    public Coordenada floor(){
        return new Coordenada(Math.floor(this.x),Math.floor(this.y));
    }
    public Coordenada top(){
        return new Coordenada(Math.ceil(this.x),Math.ceil(this.y));
    }
    public Coordenada Add(Coordenada c){
        return new Coordenada(this.x+c.x,this.y+c.y);
    }
    public Coordenada Sub(Coordenada c){
        return new Coordenada(this.x-c.x,this.y-c.y);
    }
    public Coordenada Mul(double d){
        return new Coordenada(this.x*d,this.y*d);
    }
    public Coordenada compare(Coordenada c){
        return new Coordenada(Double.compare(this.x,c.x), Double.compare(this.y,c.y));
    }

}
