#include <stdio.h>

int nhapdayso(int a[]);
int timmax(int a[], int n);

int main() {
    int a[100], b[100];
    int n1, n2;
    int max1, max2, maxall;

    printf("Nhập dãy số thứ nhất:\n");
    n1 = nhapdayso(a);

    printf("\nNhập dãy số thứ hai:\n");
    n2 = nhapdayso(b);

    max1 = timmax(a, n1);
    max2 = timmax(b, n2);

    if (max1 > max2)
        maxall = max1;
    else
        maxall = max2;

    printf("\nGiá trị lớn nhất trong cả hai dãy số là: %d\n", maxall);

    return 0;
}

int nhapdayso(int a[]) {
    int n, i;

    printf("Nhập số lượng phần tử của dãy: ");
    scanf("%d", &n);

    printf("Nhập dãy số: ");
    for (i = 0; i < n; i++) {
        scanf("%d", &a[i]);
    }

    return n;
}

int timmax(int a[], int n) {
    int max = a[0];
    int i;

    for (i = 1; i < n; i++) {
        if (a[i] > max)
            max = a[i];
    }

    return max;
}