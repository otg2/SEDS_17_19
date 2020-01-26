clc;
E = csvread('example1.dat');
%E = csvread('example2.dat');
k = 4;
cc=hsv(k);

col1 = E(:,1);
col2 = E(:,2);
max_ids = max(max(col1,col2));
As= sparse(col1, col2, 1, max_ids, max_ids); 
A = full(As);

D = diag(sum(A,2));

L = (D^(-0.5))*A*(D^(-0.5));

[X,eigVs] = eigs(L,k);

% normalizing
Y = X./sqrt(sum(X.^2,2));
%for i=1:max_ids
	%for j=1:k
		%Y(i,j) = X(i,j)/ norm(X(i,:)); 
	%end
%end

[idx,~] = kmeans(Y,k);

G = graph(A,'OmitSelfLoops');
p = plot(G,'layout','force','Marker','.','MarkerSize',10);
title('Graph');
axis equal

for i=1:k
    highlight(p,find(idx==i),'NodeColor',cc(i,:))
end

% plot 
figure,
hold on;
for i=1:size(A,1)
  c = idx(i,1);
  for j=1:size(A,2)  
    if A(i,j) == 1
        plot(i,j,'color', cc(c,:), 'marker', '+');
    end  
  end  
end
hold off;
title('Clustered Data');