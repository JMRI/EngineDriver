/*
Adapted from https://www.geeksforgeeks.org/array-implementation-of-queue-simple/
 */
package jmri.enginedriver.util;

import android.util.Log;

public final class ArrayQueue {
    private int front, rear, capacity, lastValueAdded;
    private int[] queue;

    public ArrayQueue(int Capacity) {
        front = rear = 0;
        capacity = Capacity;
        queue = new int[capacity];
        lastValueAdded = -1;
    }

    // insert an element at the rear of the queue
    public boolean enqueue(int data) {
        boolean rslt = false;
        // check queue is full or not
        if (capacity == rear) {
            Log.d("Engine_Driver", "ArrayQueue: enqueue: Queue is full");
        } else {  // insert element at the rear
            if (data!=lastValueAdded) { // don't add the same value again
                queue[rear] = data;
                rear++;
                lastValueAdded = data;
                rslt = true;
            } else {
                Log.d("Engine_Driver", "ArrayQueue: enqueue: Queue already contains value: "+data);
            }
        }
        return rslt;
    }

    public boolean enqueueWithIntermediateSteps(int data) {
        boolean rslt = false;
        // check queue is full or not
        if (capacity == rear) {
            Log.d("Engine_Driver", "ArrayQueue: enqueueWithIntermediateSteps: Queue is full");
        } else {  // insert element at the rear
            if (data!=lastValueAdded) { // don't add the same value again
                if (Math.abs(Math.abs(data) - Math.abs(lastValueAdded)) > 1) {
                    if (lastValueAdded < data) {
                        for (int i = lastValueAdded + 1; i <= data; i++) {
                            enqueue(i);
                        }
                    } else {
                        for (int i = lastValueAdded - 1; i >= data; i--) {
                            enqueue(i);
                        }
                    }
                    rslt = true;
                } else {
                    queue[rear] = data;
                    rear++;
                    lastValueAdded = data;
                    rslt = true;
                }
//            } else {
//                Log.d("Engine_Driver", "ArrayQueue: enqueueWithIntermediateSteps: Queue already contains value: "+data);
            }
        }
        return rslt;
    }


    // delete an element from the front of the queue
    public void dequeue() {
        // if queue is empty
        if (front == rear) {
            Log.d("Engine_Driver", "ArrayQueue: dequeue: Queue is Empty");
        } else { // shift all the elements from index 2 till rear // to the right by one
            if (rear - 1 >= 0) {
                System.arraycopy(queue, 1, queue, 0, rear - 1);
            }
            // store 0 at rear indicating there's no element
            if (rear < capacity) {
                queue[rear] = 0;
            }
            // decrement rear
            rear--;
        }
    }

    // empty the queue
    public void emptyQueue() {
        front = 0;
        rear = 0;
    }

    // print queue elements
    public String displayQueue() {
        int i;
        if (front == rear) {
            return "Queue is Empty";
        }
        // traverse front to rear and print elements
        String rslt = "";
        for (i = front; i < rear; i++) {
            rslt += i + ":" + queue[i] + ", ";
        }
        return " count: " + queueCount() +" queue: " + rslt;
    }

    // get the element from the front of the queue
    public int frontOfQueue() {
        int rslt = -1;
        if (front == rear) {
//            Log.d("Engine_Driver", "ArrayQueue: frontOfQueue: Queue is Empty");
            return rslt;
        }
        rslt = queue[front];
        return rslt;
    }

    // get the element from the end of the queue
    public int endOfQueue() {
        int rslt = -1;
        if (front == rear) {
//            Log.d("Engine_Driver", "ArrayQueue: endOfQueue: Queue is Empty");
            return rslt;
        }
        rslt = queue[rear];
        return rslt;
    }

    public int queueCount() {
        return rear - front;
    }

    public int getLastValueAdded() {
        return lastValueAdded;
    }
}
