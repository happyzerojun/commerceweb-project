import React, { useEffect, useState } from 'react';
import { productService } from '../services/productService';

const ProductList = () => {
  const [products, setProducts] = useState([]);

  useEffect(() => {
    // 1. 백엔드 API 호출
    productService.getProducts()
      .then(response => {
        setProducts(response.data); // 2. 성공 시 상태 업데이트
      })
      .catch(error => {
        console.error("데이터 로딩 실패:", error);
      });
  }, []);

  return (
    <div style={{ padding: '20px' }}>
      <h2>상품 목록</h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px' }}>
        {products.map(product => (
          <div key={product.id} style={{ border: '1px solid #ccc', padding: '10px' }}>
            <h4>{product.name}</h4>
            <p>{product.price.toLocaleString()}원</p>
            <p>{product.description}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ProductList;