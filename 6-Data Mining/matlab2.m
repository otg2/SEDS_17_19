% Load data
myData = csvread("example1.dat")
% Sigma not used?
sigma = 1;
% Code for adjacency matrix
col1 = myData(:,1);
col2 = myData(:,2);
max_ids = max(max(col1,col2));
As= sparse(col1, col2, 1, max_ids, max_ids); 
A = full(As)


% Diagonal matrix
D = diag(sum(A, 1));
% Laplacian matrix
L = D^(-0.5) * A * D^(-0.5)
% Find k biggest eigenvalues
K = 4
[X, xE] = eigs(L, K);

% Normalize
Y = zeros(size(X));
for i=1:max_ids
	for j=1:K
		Y(i,j) = X(i,j)/ norm(X(i,:)); 
	end
end
% Kmeans
[idx, ~] = kmeans(Y, K);

% Color and plot
colors = {'m', 'b', 'c', 'r', 'y'};
hold on;
for i=1:size(A,1)
  z = idx(i,1);
  for j=1:size(A,2)  
    if A(i,j) == 1
        plot(i,j,'color', colors{z}, 'marker', '*');
    end  
  end  
end
hold off;
title('Clustering Results using K-means');
grid on;shg